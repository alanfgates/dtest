/*
 * Copyright (C) 2018 Hortonworks Inc.
 *
 * Licenced under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dtest.core;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dtest.core.impl.PluginFactory;
import org.dtest.core.impl.ProcessResults;
import org.dtest.core.impl.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DockerTest {
  private static final Logger LOG = LoggerFactory.getLogger(DockerTest.class);
  private static final String SUMMARY_LOG = "summary";
  static final String DTEST_HOME = "DTEST_HOME";
  public static final String EXEC_LOG = "dtest-exec"; // for log entries by dtest
  // Simultaneous number of containers to run
  protected static final String CFG_NUM_CONTAINERS = "dtest.number.containers";

  static {
    Config.setDefaultValue(CFG_NUM_CONTAINERS, "2");
  }

  private ContainerClient docker;
  private int numContainers;
  private PrintStream out;
  private PrintStream err;
  private String baseDir;

  public DockerTest(PrintStream out, PrintStream err) {
    this.out = out;
    this.err = err;
  }

  /**
   * Parse the arguments.
   * @param args Command line arguments.
   * @return A description of the build, or null if the parsing failed.
   */
  @SuppressWarnings("static-access")
  public BuildInfo parseArgs(String[] args) {
    CommandLineParser parser = new GnuParser();

    Options opts = new Options();

    opts.addOption(OptionBuilder
        .withLongOpt("base-directory")
        .withDescription("directory to build dockerfile in")
        .isRequired()
        .hasArg()
        .create("d"));

    opts.addOption(OptionBuilder
        .withLongOpt("build-label")
        .withDescription("build label, changing this will force a new container to be built")
        .hasArg()
        .isRequired()
        .create("l"));

    opts.addOption(OptionBuilder
        .withLongOpt("test-profile")
        .withDescription("profiles available for testing, usually tied to a branch, current " +
            "available ones are: " + findAvailableProfiles())
        .hasArg()
        .isRequired()
        .create("p"));

    opts.addOption(OptionBuilder
        .withLongOpt("no-cleanup")
        .withDescription("do not cleanup docker containers and image after build")
        .create("m"));

    CommandLine cmd;
    try {
      cmd = parser.parse(opts, args);
    } catch (ParseException e) {
      LOG.error("Failed to parse command line: ", e);
      usage(opts);
      return null;
    }

    numContainers = Config.getAsInt(DockerTest.CFG_NUM_CONTAINERS);
    baseDir = cmd.getOptionValue("d");
    try {
      CodeSource codeSource = CodeSource.getInstance();
      BuildInfo info = new BuildInfo(codeSource, cmd.getOptionValue("l").toLowerCase(), cmd.getOptionValue("p"));
      info.setCleanupAfter(!cmd.hasOption("m"));
      return info;
    } catch (IOException e) {
      err.println(e.getMessage());
      LOG.error("Failed to build BuildInfo", e);
      return null;
    }
  }

  public int runBuild(BuildInfo info) {
    DTestLogger logger = null;
    int rc;
    try {
      docker = ContainerClient.getInstance();
      docker.setBuildInfo(info);
      String dir = info.buildDir(baseDir);
      logger = new DTestLogger(dir);
      try {
        buildDockerImage(info, logger);
      } catch (IOException e) {
        String msg = "Failed to build docker image, might mean your code doesn't compile";
        err.println(msg + "  See log for details.");
        LOG.error(msg, e);
        return 1;
      }
      try {
        rc = runContainers(info, logger, numContainers);
        packageLogsAndCleanup(info, logger);
      } catch (IOException e) {
        String msg = "Failed to run one or more of the containers.";
        err.println(msg + "  See log for details.");
        LOG.error(msg, e);
        return -1;
      }
    } catch (IOException e) {
      String msg = "Failed to open the logger.  This often means you gave a bogus output directory.";
      err.println(msg);
      LOG.error(msg, e);
      return -1;
    } finally {
      if (logger != null) {
        try {
          logger.close();
        } catch (IOException e) {
          String msg = "Failed to close the logger.";
          err.println(msg);
          LOG.error(msg);
          rc = 1;
        }
      }
    }
    return rc;
  }

  private void usage(Options opts) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("docker-test", opts);
  }

  private String findAvailableProfiles() {
    // This is a lame implementation, in that it has to know to look for something before it can
    // find it, but I'm not sure of a better way to do it.
    String[] possibilies = {"master-profile.yaml", "branch-3-profile.yaml", "branch-2-profile.yaml"};
    StringBuilder buf = new StringBuilder();
    boolean first = true;
    for (String possibility : possibilies) {
      URL yamlFile = getClass().getClassLoader().getResource(possibility);
      if (yamlFile != null) {
        if (first) first = false;
        else buf.append(", ");
        buf.append(new File(yamlFile.getFile()).getName());
      }
    }
    return buf.toString();
  }

  private void buildDockerImage(BuildInfo info, DTestLogger logger)
      throws IOException {
    docker.defineImage();
    docker.buildImage(info.getDir(), logger);
  }

  private int runContainers(final BuildInfo info, final DTestLogger logger, int numContainers)
      throws IOException {
    ContainerCommandList taskCmds = ContainerCommandList.getInstance();
    taskCmds.buildContainerCommands(docker, info, logger);

    final ResultAnalyzer analyzer = ResultAnalyzer.getInstance();
    // I don't need the return value, but by having one I can use the Callable interface instead
    // of Runnable, and Callable catches exceptions for me and passes them back.
    List <Future<Integer>> tasks = new ArrayList<>(taskCmds.size());
    ExecutorService executor = Executors.newFixedThreadPool(numContainers);
    for (ContainerCommand taskCmd : taskCmds) {
      tasks.add(executor.submit(() -> {
        ContainerResult result = docker.runContainer(taskCmd, logger);
        analyzer.analyzeLog(result);
        StringBuilder statusMsg = new StringBuilder("Task ")
            .append(result.getCmd().containerSuffix())
            .append(' ');
        switch (result.getAnalysisResult()) {
        case TIMED_OUT:
          statusMsg.append(" had TIMEOUTS");
          break;

        case FAILED:
          statusMsg.append(" FAILED to run to completion");
          break;

        case SUCCEEDED:
          statusMsg.append(" SUCCEEDED (does not mean all tests passed)");
          break;

        default:
          throw new RuntimeException("Unexpected state");
        }
        logger.write(result.getCmd().containerSuffix(), statusMsg.toString());

        // Copy log files from any failed tests to a directory specific to this container
        if (result.getLogFilesToFetch() != null && !result.getLogFilesToFetch().isEmpty()) {
          File logDir = new File(info.getDir(), result.getCmd().containerSuffix());
          LOG.info("Creating directory " + logDir.getAbsolutePath() + " for logs from container "
              + result.getCmd().containerSuffix());
          logDir.mkdir();
          docker.copyLogFiles(result, logDir.getAbsolutePath(), logger);
        }
        docker.removeContainer(result, logger);
        return 1;
      }));
    }

    boolean runSucceeded = true;
    for (Future<Integer> task : tasks) {
      try {
        task.get();
      } catch (InterruptedException e) {
        LOG.error("Interrupted while waiting for containers to finish, assuming I was" +
            " told to quit.", e);
        runSucceeded = false;
      } catch (ExecutionException e) {
        LOG.error("Got an exception while running container, that's generally bad", e);
        runSucceeded = false;
      }
    }

    runSucceeded &= analyzer.runSucceeded();

    executor.shutdown();
    if (analyzer.getErrors().size() > 0) {
      logger.writeAndPrint(SUMMARY_LOG, "All Errors:", out);
      for (String error : analyzer.getErrors()) {
        logger.writeAndPrint(SUMMARY_LOG, error, out);
      }
    }
    if (analyzer.getFailed().size() > 0) {
      logger.writeAndPrint(SUMMARY_LOG, "All Failures:", out);
      for (String failure : analyzer.getFailed()) {
        logger.writeAndPrint(SUMMARY_LOG, failure, out);
      }
    }
    StringBuilder msg = new StringBuilder("Test run ");
    int rc = 0;
    if (!runSucceeded) {
      msg.append("FAILED, this can mean tests failed or mvn commands failed to " +
          "execute properly.\n");
      rc = 1;
    } else if (analyzer.hadTimeouts()) {
      msg.append("HAD TIMEOUTS.  Following numbers are incomplete.\n");
      rc = -1;
    } else {
      msg.append("SUCCEEDED");
    }
    msg.append("Final counts: Succeeded: ")
        .append(analyzer.getSucceeded())
        .append(", Errors: ")
        .append(analyzer.getErrors().size())
        .append(", Failures: ")
        .append(analyzer.getFailed().size());
    logger.writeAndPrint(SUMMARY_LOG, msg.toString(), out);
    return rc;
  }

  private void packageLogsAndCleanup(BuildInfo info, DTestLogger logger) throws IOException {
    ProcessResults res = Utils.runProcess("tar", 60, logger, "tar", "zcf",
        info.getLabel() + ".tgz", "-C", baseDir, info.getLabel());
    if (res.rc != 0) {
      throw new IOException("Failed to tar up logs, error " + res.rc + " msg: " + res.stderr);
    }
    docker.removeImage(logger);

  }

  /**
   * This calls System.exit, don't call it if you're using a tool.  Use
   * {@link #runBuild(BuildInfo)} instead.
   * @param args command line arguments.
   */
  public static void main(String[] args) {
    // Read the config file
    try {
      Config.fromConfigFile();
    } catch (IOException e) {
      System.err.println("Failed to read config file: " + e.getMessage());
      System.exit(-1);
    }

    DockerTest test = new DockerTest(System.out, System.err);
    BuildInfo build = test.parseArgs(args);
    int rc = (build == null) ? 1 : test.runBuild(build);
    System.exit(rc);
  }
}
