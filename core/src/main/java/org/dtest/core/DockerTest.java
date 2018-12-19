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

import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dtest.core.impl.ProcessResults;
import org.dtest.core.impl.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DockerTest {
  private static final Logger LOG = LoggerFactory.getLogger(DockerTest.class);
  private static final String SUMMARY_LOG = "summary";
  public static final String EXEC_LOG = "dtest-exec"; // for log entries by dtest
  // Simultaneous number of containers to run
  static final String CFG_CORE_DOCKERTEST_NUMCONTAINERS = "dtest.core.dockertest.numcontainers";

  private ContainerClient docker;
  private PrintStream out;
  private PrintStream err;
  private Config cfg;
  private BuildInfo buildInfo;
  private String cfgDir;
  private boolean cleanupAfter = true;
  private DTestLogger logger;

  public DockerTest(PrintStream out, PrintStream err) {
    this.out = out;
    this.err = err;
  }

  /**
   * Setup the configuration.  Call this first.
   * @param confDir directory with configuration file in it.
   * @param override properties we were passed that take precedence over values in the file.
   * @throws IOException if we fail to read the config file.
   */
  public void buildConfig(String confDir, Properties override) throws IOException {
    cfg = new Config(confDir, override);
  }

  /**
   * This is for testing.  You likely want {@link #buildConfig(String, Properties)}.
   * @param props properties to put in config file
   */
  @VisibleForTesting
  public void buildConfig(Properties props) {
    cfg = new Config(props);
  }

  /**
   * Parse the arguments.
   * @param args Command line arguments.
   */
  @SuppressWarnings("static-access")
  private void parseArgs(String[] args) {
    CommandLineParser parser = new GnuParser();

    Options opts = new Options();

    opts.addOption(OptionBuilder
        .withLongOpt("conf-dir")
        .withDescription("Directory where configuration and build profile files are")
        .hasArg()
        .isRequired()
        .create("c"));

    opts.addOption(OptionBuilder
        .withLongOpt("no-cleanup")
        .withDescription("do not cleanup docker containers and image after build")
        .create("m"));

    CommandLine cmd;
    try {
      cmd = parser.parse(opts, args);
      cleanupAfter = !cmd.hasOption("m");
      cfgDir = cmd.getOptionValue("c");
    } catch (ParseException e) {
      LOG.error("Failed to parse command line: ", e);
      usage(opts);
    }
  }

  /**
   * Setup the build information.  Call this after {@link #buildConfig(String, Properties)} and before
   * {@link #runBuild()}.
   */
  public void prepareBuild() throws IOException {
    CodeSource codeSource = CodeSource.getInstance(cfg);
    buildInfo = new BuildInfo(cfgDir, codeSource, cleanupAfter);
    buildInfo.setConfig(cfg);
  }

  public int runBuild() {
    int rc;
    try {
      docker = ContainerClient.getInstance(cfg);
      docker.setBuildInfo(buildInfo);
      String dir = buildInfo.buildDir();
      logger = new DTestLogger(dir);
      try {
        buildDockerImage();
      } catch (IOException e) {
        String msg = "Failed to build docker image, might mean your code doesn't compile";
        err.println(msg + "  See log for details.");
        LOG.error(msg, e);
        return 1;
      }
      try {
        rc = runContainers();
        packageLogsAndCleanup();
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

  private void buildDockerImage()
      throws IOException {
    docker.defineImage();
    docker.buildImage(buildInfo.getDir(), logger);
  }

  private int runContainers()
      throws IOException {
    ContainerCommandList taskCmds = ContainerCommandList.getInstance(cfg);
    taskCmds.buildContainerCommands(docker, buildInfo, logger);

    final ResultAnalyzer analyzer = ResultAnalyzer.getInstance(cfg);
    // I don't need the return value, but by having one I can use the Callable interface instead
    // of Runnable, and Callable catches exceptions for me and passes them back.
    List <Future<Integer>> tasks = new ArrayList<>(taskCmds.getCmds().size());
    ExecutorService executor = Executors.newFixedThreadPool(cfg.getAsInt(CFG_CORE_DOCKERTEST_NUMCONTAINERS, 2));
    for (ContainerCommand taskCmd : taskCmds.getCmds()) {
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
          File logDir = new File(buildInfo.getDir(), result.getCmd().containerSuffix());
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

  private void packageLogsAndCleanup() throws IOException {
    ProcessResults res = Utils.runProcess("tar", 60, logger, "tar", "zcf",
        buildInfo.getLabel() + ".tgz", "-C", buildInfo.getBaseDir(), buildInfo.getLabel());
    if (res.rc != 0) {
      throw new IOException("Failed to tar up logs, error " + res.rc + " msg: " + res.stderr);
    }
    docker.removeImage(logger);

  }

  /**
   * This calls System.exit, don't call it if you're using a tool.  Instead call
   * {@link #buildConfig(String, Properties)} then {@link #prepareBuild()} and then {@link #runBuild()}.
   * @param args command line arguments.
   */
  public static void main(String[] args) {
    DockerTest test = new DockerTest(System.out, System.err);
    test.parseArgs(args);
    int rc = 0;
    try {
      test.buildConfig(test.cfgDir, System.getProperties());
      test.prepareBuild();
      rc = test.runBuild();
    } catch (IOException e) {
      rc = 1;
      LOG.error("Failed to run", e);
      System.err.println("Failed, see logs for more details: " + e.getMessage());
    }
    System.exit(rc);
  }
}
