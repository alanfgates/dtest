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
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * DockerTest is the main class.  It can be accessed via the command line or called from a tool.  If using from
 * a tool you need to first setup the configuration via {@link #buildConfig(Properties)}, the log
 * using {@link #setLogger(DTestLogger)}, then run the build using {@link #runBuild()}.
 */
public class DockerTest {
  /**
   * Number of containers to run.  Defaults to 2.
   */
  public static final String CFG_DOCKERTEST_NUMCONTAINERS = "dtest.core.dockertest.numcontainers";
  private static final int CFG_DOCKERTEST_NUMCONTAINERS_DEFAULT = 2;

  private static final String TESTONLY_CFGDIR = "dtest.testonly.conf.dir";

  private static final String SUMMARY_LOG = "summary";
  public static final String EXEC_LOG = "dtest-exec"; // for log entries by dtest

  private ContainerClient docker;
  private Config cfg;
  private BuildInfo buildInfo;
  private File cfgDir;
  private String profile;
  private boolean cleanupAfter = true;
  private DTestLogger log;
  private String repo;
  private String branch;
  private String buildDir;
  //private Map<String, String> logLinks; // HTML links to the logs
  private Reporter reporter;

  @VisibleForTesting boolean isCleanupAfter() {
    return cleanupAfter;
  }

  public DockerTest() {
    //logLinks = new ConcurrentHashMap<>();
  }

  /**
   * Setup the configuration.  This should be called before other methods in this class.
   * @param override properties that take precedence over values in dtest.properties.
   * @throws IOException if the config file cannot be read.
   */
  public void buildConfig(Properties override) throws IOException {
    cfg = new Config(cfgDir, override);
  }

  /**
   *
   * @param cleanupAfter whether to cleanup after the build.  Usually you want this to be true (the default) to avoid leaving
   *                     docker images and containers lying around.  But setting it to false can be useful if you
   *                     are seeing unexpected errors in your run and want to debug them.
   */
  public void setCleanupAfter(boolean cleanupAfter) {
    this.cleanupAfter = cleanupAfter;
  }

  public void setRepo(String repo) {
    this.repo = repo;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  /**
   * Set the logger implementation for this instance.  This must be called before {@link #runBuild()}.
   * @param log logger implementation.
   */
  public void setLogger(DTestLogger log) {
    this.log = log;
  }

  @VisibleForTesting boolean parseArgs(String[] args) {
    // The logs have not been set up yet when this method is called.  Also, this method should only be used
    // from the command line.  So any errors encountered here need to be printed out rather than sent to the logs.
    CommandLineParser parser = new DefaultParser();

    Options opts = new Options();

    opts.addOption(Option.builder("b")
        .longOpt("branch")
        .desc("Source control branch to build from")
        .hasArg()
        .build());

    opts.addOption(Option.builder("d")
        .longOpt("build-dir")
        .desc("Build directory.  This should be unique to the build.")
        .hasArg()
        .required()
        .build());

    opts.addOption(Option.builder("n")
        .longOpt("no-cleanup")
        .desc("do not cleanup docker containers and image after build")
        .build());

    opts.addOption(Option.builder("p")
        .longOpt("profile")
        .desc("Profile to build with")
        .hasArg()
        .required()
        .build());

    opts.addOption(Option.builder("r")
        .longOpt("repo")
        .desc("Source control repository to checkout code from")
        .hasArg()
        .build());

    CommandLine cmd;
    try {
      cmd = parser.parse(opts, args);
      cleanupAfter = !cmd.hasOption("n");
      profile = cmd.getOptionValue("p");
      if (cmd.hasOption("b")) branch = cmd.getOptionValue("b");
      if (cmd.hasOption("r")) repo = cmd.getOptionValue("r");
      buildDir = cmd.getOptionValue('d');
    } catch (ParseException e) {
      System.err.println("Failed to parse command line: " + e.getMessage());
      usage(opts);
      return false;
    }

    // Find our configuration file.
    try {
      determineCfgDir();
    } catch (IOException e) {
      System.err.println("Could not determine configuration directory: " + e.getMessage());
      return false;
    }

    return true;
  }

  /**
   * Run the build.  This may take a while (obviously) and will launch a number of threads.  You must call one of
   * the buildConfig methods and {@link #setLogger(DTestLogger)} before calling this.
   * @return BuildState indicating the status of the build.
   */
  public BuildState runBuild() {
    ResultAnalyzer result = null;
    boolean mightHaveBuiltImage = false;
    try {
      log.info("Going to build branch " + branch + " from repo " + repo + " using profile in " + profile);
      BuildYaml yaml = BuildYaml.readYaml(cfgDir, cfg, log, repo, profile, branch);
      CodeSource codeSource = CodeSource.getInstance(cfg, log);
      buildInfo = new BuildInfo(yaml, codeSource, cleanupAfter, buildDir);
      buildInfo.setConfig(cfg).setLog(log);
      reporter = Reporter.getInstance(cfg, log)
          .setRepo(repo)
          .setBranch(branch)
          .setProfile(profile)
          .setBuildInfo(buildInfo);
      docker = ContainerClient.getInstance(cfg, log);
      docker.setBuildInfo(buildInfo);
      ContainerCommandFactory cmdFactory = ContainerCommandFactory.getInstance(cfg, log);
      mightHaveBuiltImage = true;
      docker.buildImage(cmdFactory);
      result = runContainers(cmdFactory);
      outputResults(result);
      return result.getBuildState();
    } catch (IOException e) {
      log.error("Failed to run the build", e);
      // we might have failed before state got set
      BuildState state = null;
      state = (result == null || result.getBuildState() == null) ? new BuildState() : result.getBuildState();
      state.fail();
      return state;
    } catch (Throwable t) {
      // Catch this in case we got a NPE or something, otherwise we don't get the output in the logs.
      log.error("Failed to run build", t);
      throw t;
    } finally {
      if (mightHaveBuiltImage && buildInfo.shouldCleanupAfter()) {
        try {
          docker.removeImage();
        } catch (IOException e) {
          // Not much that can be done here
          log.warn("Failed to remove docker image, which can pollute your docker registry and cause future builds to" +
              "fail if docker thinks it has already built the requested image.");
        }
      }

    }
  }

  private void usage(Options opts) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("docker-test", opts);
  }

  @VisibleForTesting
  void determineCfgDir() throws IOException {
    // For testing we set the cfg value as System property rather than using DTEST_HOME
    String testing = System.getProperty(TESTONLY_CFGDIR );
    if (testing != null) {
      cfgDir = new File(testing);
    } else {
      String dtestHome = System.getenv("DTEST_HOME");
      if (dtestHome == null || dtestHome.isEmpty()) {
        throw new IOException("You must set DTEST_HOME before running bin/dtest");
      }
      cfgDir = new File(dtestHome, "conf");
    }
    if (!cfgDir.exists()) {
      throw new IOException(cfgDir.getAbsolutePath() + ": no such directory");
    }
    if (!cfgDir.isDirectory()) {
      throw new IOException(cfgDir.getAbsolutePath() + " is not a directory");
    }
  }

  private ResultAnalyzer runContainers(ContainerCommandFactory cmdFactory)
      throws IOException {
    log.debug("Beginning our attack run");
    cmdFactory.buildContainerCommands(docker, buildInfo);

    final ResultAnalyzer analyzer = ResultAnalyzer.getInstance(cfg, log);
    // I don't need the return value, but by having one I can use the Callable interface instead
    // of Runnable, and Callable catches exceptions for me and passes them back.
    List <Future<Integer>> tasks = new ArrayList<>(cmdFactory.getCmds().size());
    ExecutorService executor =
        Executors.newFixedThreadPool(cfg.getAsInt(CFG_DOCKERTEST_NUMCONTAINERS, CFG_DOCKERTEST_NUMCONTAINERS_DEFAULT));
    for (ContainerCommand taskCmd : cmdFactory.getCmds()) {
      log.debug("Going to run task " + taskCmd.containerSuffix());
      tasks.add(executor.submit(() -> {
        ContainerResult result = docker.runContainer(taskCmd);
        analyzer.analyzeLog(result, buildInfo.getYaml());
        StringBuilder statusMsg = new StringBuilder("Task ")
            .append(result.getCmd().containerSuffix())
            .append(' ');
        log.debug("Result from running " + taskCmd.containerSuffix() + " his " + result.getAnalysisResult());
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
        reporter.addFailedTests(docker, result);
        if (buildInfo.shouldCleanupAfter()) docker.removeContainer(result);
        return 1;
      }));
    }

    BuildState buildState = analyzer.getBuildState();
    for (Future<Integer> task : tasks) {
      try {
        task.get();
      } catch (InterruptedException e) {
        log.error("Interrupted while waiting for containers to finish, assuming I was" +
            " told to quit.", e);
        buildState.fail();
      } catch (ExecutionException e) {
        log.error("Got an exception while running container, that's generally bad", e);
        buildState.fail();
      }
    }
    assert buildState.getState() != BuildState.State.NOT_INITIALIZED;

    executor.shutdown();
    return analyzer;
  }

  private void outputResults(ResultAnalyzer analyzer) throws IOException {
    reporter.summarize(analyzer);
    reporter.publish();

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
    log.info(SUMMARY_LOG, analyzer.getBuildState().getState().getSummary());
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
        test.buildConfig(System.getProperties());
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
