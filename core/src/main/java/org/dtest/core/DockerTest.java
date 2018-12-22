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

/**
 * DockerTest is the main class.  It can be accessed via the command line or called from a tool.  If using from
 * a tool you need to first setup the configuration via {@link #buildConfig(String, Properties)}, then
 * run the build using {@link #runBuild()}.
 */
public class DockerTest {
  /**
   * Number of containers to run.  Defaults to 2.
   */
  public static final String CFG_DOCKERTEST_NUMCONTAINERS = "dtest.core.dockertest.numcontainers";
  private static final int CFG_DOCKERTEST_NUMCONTAINERS_DEFAULT = 2;

  /**
   * Where to drop the tar file produced by the build.  Defaults to java.io.tmpdir.
   */
  public static final String CFG_DOCKERTEST_RESULTLOCATION = "dtest.core.dockertest.resultlocation";
  private static final String CFG_DOCKERTEST_RESULTLOCATION_DEFAULT = System.getProperty("java.io.tmpdir");

  private static final String SUMMARY_LOG = "summary";
  public static final String EXEC_LOG = "dtest-exec"; // for log entries by dtest

  private ContainerClient docker;
  private Config cfg;
  private BuildInfo buildInfo;
  private String cfgDir;
  private boolean cleanupAfter = true;
  private DTestLogger log;

  public DockerTest() {
  }

  /**
   * Setup the configuration.  Call this first.
   * @param confDir directory with configuration file in it.
   * @param override properties we were passed that take precedence over values in the file.
   * @throws IOException if we fail to read the config file.
   */
  public void buildConfig(String confDir, Properties override) throws IOException {
    cfgDir = confDir; // If you came from the outside and not main this won't be set yet.
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

  public void setLogger(DTestLogger log) {
    this.log = log;
  }

  @SuppressWarnings("static-access")
  private boolean parseArgs(String[] args) {
    // The logs have not been set up yet when this method is called.  Also, this method should only be used
    // from the command line.  So any errors encountered here need to be printed out rather than sent to the logs.
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
      return true;
    } catch (ParseException e) {
      System.err.println("Failed to parse command line: " + e.getMessage());
      usage(opts);
      return false;
    }
  }

  /**
   * Run the build.  This may take a while (obviously) and will launch a number of threads.
   * @return status of the build, 0 for success 1 for failure, -1 for error.
   */
  public BuildState runBuild() {
    BuildState state = null;
    try {
      CodeSource codeSource = CodeSource.getInstance(cfg, log);
      buildInfo = new BuildInfo(cfgDir, codeSource, cleanupAfter);
      buildInfo.setConfig(cfg);
      docker = ContainerClient.getInstance(cfg, log);
      docker.setBuildInfo(buildInfo);
      ContainerCommandFactory cmdFactory = ContainerCommandFactory.getInstance(cfg, log);
      docker.buildImage(cmdFactory);
      state = runContainers(cmdFactory);
      packageLogsAndCleanup();
      return state;
    } catch (IOException e) {
      log.error("Failed to run the build", e);
      // we might have failed before state got set
      if (state == null) state = new BuildState();
      state.fail();
      return state;
    }
  }

  private void usage(Options opts) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("docker-test", opts);
  }

  private BuildState runContainers(ContainerCommandFactory cmdFactory)
      throws IOException {
    cmdFactory.buildContainerCommands(docker, buildInfo);

    final ResultAnalyzer analyzer = ResultAnalyzer.getInstance(cfg, log);
    // I don't need the return value, but by having one I can use the Callable interface instead
    // of Runnable, and Callable catches exceptions for me and passes them back.
    List <Future<Integer>> tasks = new ArrayList<>(cmdFactory.getCmds().size());
    ExecutorService executor =
        Executors.newFixedThreadPool(cfg.getAsInt(CFG_DOCKERTEST_NUMCONTAINERS, CFG_DOCKERTEST_NUMCONTAINERS_DEFAULT));
    for (ContainerCommand taskCmd : cmdFactory.getCmds()) {
      tasks.add(executor.submit(() -> {
        ContainerResult result = docker.runContainer(taskCmd);
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
        log.info(result.getCmd().containerSuffix(), statusMsg.toString());

        // Copy log files from any failed tests to a directory specific to this container
        if (result.getLogFilesToFetch() != null && !result.getLogFilesToFetch().isEmpty()) {
          File logDir = new File(buildInfo.getBuildDir(), result.getCmd().containerSuffix());
          log.info("Creating directory " + logDir.getAbsolutePath() + " for logs from container "
              + result.getCmd().containerSuffix());
          logDir.mkdir();
          docker.copyLogFiles(result, logDir.getAbsolutePath());
        }
        docker.removeContainer(result);
        return 1;
      }));
    }

    BuildState buildState = new BuildState();
    for (Future<Integer> task : tasks) {
      try {
        task.get();
      } catch (InterruptedException e) {
        log.error("Interrupted while waiting for containers to finish, assuming I was" +
            " told to quit.", e);
        buildState.timeout();
      } catch (ExecutionException e) {
        log.error("Got an exception while running container, that's generally bad", e);
        buildState.fail();
      }
    }

    executor.shutdown();
    buildState.update(analyzer.getBuildState());
    if (analyzer.getErrors().size() > 0) {
      log.info(SUMMARY_LOG, "All Errors:");
      for (String error : analyzer.getErrors()) {
        log.info(SUMMARY_LOG, error);
      }
    }
    if (analyzer.getFailed().size() > 0) {
      log.info(SUMMARY_LOG, "All Failures:");
      for (String failure : analyzer.getFailed()) {
        log.info(SUMMARY_LOG, failure);
      }
    }
    log.info(SUMMARY_LOG, "Final counts: Succeeded: " + analyzer.getSucceeded() +
        ", Errors: " + analyzer.getErrors().size() +
        ", Failures: " + analyzer.getFailed().size());
    log.info(SUMMARY_LOG, buildState.getState().getSummary());
    return buildState;
  }

  private void packageLogsAndCleanup() throws IOException {
    ProcessResults res = Utils.runProcess("tar", 60, log, "tar", "zcf",
        getResultsDir() + buildInfo.getLabel() + ".tgz", "-C", buildInfo.getBaseDir(), buildInfo.getLabel());
    if (res.rc != 0) {
      throw new IOException("Failed to tar up logs, error " + res.rc + " msg: " + res.stderr);
    }
    docker.removeImage();

  }

  private String getResultsDir() {
    return cfg.getAsString(CFG_DOCKERTEST_RESULTLOCATION, CFG_DOCKERTEST_RESULTLOCATION_DEFAULT) + File.separator;
  }

  /**
   * This calls System.exit, don't call it if you're using a tool.
   * @param args command line arguments.
   */
  public static void main(String[] args) {
    DockerTest test = new DockerTest();
    int rc;
    if (test.parseArgs(args)) {
      try {
        test.buildConfig(test.cfgDir, System.getProperties());
        test.setLogger(new Slf4jLogger());
        BuildState state = test.runBuild();
        switch (state.getState()) {
          case NOT_INITIALIZED:
            throw new RuntimeException("This shouldn't happen");
          case SUCCEEDED:
            rc = 0;
            break;
          case HAD_FAILURES_OR_ERRORS:
            rc = 1;
            break;
          default:
            rc = -1;
            break;
        }
      } catch (IOException e) {
        rc = 1;
        System.err.println("Failed, see logs for more details: " + e.getMessage());
      }
    } else {
      rc = -1;
    }
    System.exit(rc);
  }
}
