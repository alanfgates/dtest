/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hive.testutils.dtest;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hive.testutils.dtest.impl.ContainerResult;
import org.apache.hive.testutils.dtest.impl.DTestLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DockerTest {
  private static final Logger LOG = LoggerFactory.getLogger(DockerTest.class);
  private static final String SUMMARY_LOG = "summary";

  private ContainerClient docker;
  private ContainerCommandFactory commandFactory;
  private ResultAnalyzerFactory analyzerFactory;
  private ContainerClientFactory containerClientFactory;
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
  public BuildInfo parseArgs(String[] args) {
    CommandLineParser parser = new GnuParser();

    Options opts = new Options();
    opts.addOption(OptionBuilder
        .withLongOpt("branch")
        .withDescription("git branch to use")
        .hasArg()
        .isRequired()
        .create("b"));

    opts.addOption(OptionBuilder
        .withLongOpt("num-containers")
        .withDescription("number of simultaneous containers to run, defaults to 1")
        .hasArg()
        .create("c"));

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
        .withLongOpt("repo")
        .withDescription("git repository to use")
        .hasArg()
        .isRequired()
        .create("r"));

    CommandLine cmd;
    try {
      cmd = parser.parse(opts, args);
    } catch (ParseException e) {
      LOG.error("Failed to parse command line: ", e);
      usage(opts);
      return null;
    }

    numContainers = cmd.hasOption("c") ? Integer.parseInt(cmd.getOptionValue("c")) : 1;
    baseDir = cmd.getOptionValue("d");
    try {
      containerClientFactory = ContainerClientFactory.get();
      commandFactory = ContainerCommandFactory.get();
      analyzerFactory = ResultAnalyzerFactory.get();
    } catch (IOException e) {
      String msg = "Failed to instantiate one of the factories.";
      err.println(msg + "  See log for details.");
      LOG.error(msg, e);
      return null;
    }
    try {
      return new BuildInfo(cmd.getOptionValue("b"), cmd.getOptionValue("r"),
          cmd.getOptionValue("l").toLowerCase());
    } catch (IOException e) {
      err.println(e.getMessage());
      LOG.error("Failed to build BuildInfo", e);
      return null;
    }
  }

  public int startBuild(BuildInfo info) {
    docker = containerClientFactory.getClient(info.getLabel());
    DTestLogger logger = null;
    int rc = 1;
    try {
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
        runContainers(info, logger, numContainers, out);
        rc = 0;
      } catch (IOException e) {
        String msg = "Failed to run one or more of the containers.";
        err.println(msg + "  See log for details.");
        LOG.error(msg, e);
        return 1;
      }
    } catch (IOException e) {
      String msg = "Failed to open the logger.  This often means you gave a bogus output directory.";
      err.println(msg);
      LOG.error(msg, e);
      return 1;
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

  private void buildDockerImage(BuildInfo info, DTestLogger logger)
      throws IOException {
    int timeout = Integer.valueOf(System.getProperty(Config.IMAGE_BUILD_TIME, "30"));
    TimeUnit timeUnit = TimeUnit.valueOf(System.getProperty(Config.IMAGE_BUILD_TIME_UNIT,
        TimeUnit.MINUTES.name()));
    docker.defineImage(info.getDir(), info.getRepo(), info.getBranch(), info.getLabel());
    docker.buildImage(info.getDir(), timeout, timeUnit, logger);
  }

  private void runContainers(final BuildInfo info, final DTestLogger logger, int numContainers,
                             PrintStream out) throws IOException {
    List<ContainerCommand> taskCmds = commandFactory.getContainerCommands(docker, info.getLabel(), logger);

    final int timeout = Integer.valueOf(System.getProperty(Config.CONTAINER_RUN_TIME, "3"));
    final TimeUnit timeUnit = TimeUnit.valueOf(System.getProperty(Config.CONTAINER_RUN_TIME_UNIT,
        TimeUnit.HOURS.name()));

    final ResultAnalyzer analyzer = analyzerFactory.getAnalyzer();
    // I don't need the return value, but by having one I can use the Callable interface instead
    // of Runnable, and Callable catches exceptions for me and passes them back.
    List <Future<Integer>> tasks = new ArrayList<>(taskCmds.size());
    ExecutorService executor = Executors.newFixedThreadPool(numContainers);
    for (ContainerCommand taskCmd : taskCmds) {
      tasks.add(executor.submit(() -> {
        ContainerResult result = docker.runContainer(timeout, timeUnit, taskCmd, logger);
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
      logger.write(SUMMARY_LOG, "All Errors:");
      for (String error : analyzer.getErrors()) {
        logger.write(SUMMARY_LOG, error);
      }
    }
    if (analyzer.getFailed().size() > 0) {
      logger.write(SUMMARY_LOG, "All Failures:");
      for (String failure : analyzer.getFailed()) {
        logger.write(SUMMARY_LOG, failure);
      }
    }
    StringBuilder msg = new StringBuilder("Test run ");
    if (!runSucceeded) msg.append("FAILED.  Following numbers are probably meaningless.\n");
    else if (analyzer.hadTimeouts()) msg.append("HAD TIMEOUTS.  Following numbers are incomplete.\n");
    else msg.append("RAN ALL TESTS ");
    msg.append("Final counts: Succeeded: ")
        .append(analyzer.getSucceeded())
        .append(", Errors: ")
        .append(analyzer.getErrors().size())
        .append(", Failures: ")
        .append(analyzer.getFailed().size());
    logger.write(SUMMARY_LOG, msg.toString());
    out.println(msg.toString());
  }

  /**
   * This calls System.exit, don't call it if you're using a tool.  Use
   * {@link #startBuild(BuildInfo)} instead.
   * @param args command line arguments.
   */
  public static void main(String[] args) {
    DockerTest test = new DockerTest(System.out, System.err);
    BuildInfo build = test.parseArgs(args);
    int rc = (build == null) ? 1 : test.startBuild(build);
    System.exit(rc);
  }
}
