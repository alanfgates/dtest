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
import org.dtest.core.impl.ProcessResults;
import org.dtest.core.impl.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * DockerTest is the main class.  It can be accessed via the command line or called from a tool.  If using from
 * a tool you need to first setup the configuration via {@link #buildConfig(String, Properties)}, the log
 * using {@link #setLogger(DTestLogger)}, then run the build using {@link #runBuild()}.
 */
public class DockerTest {
  /*~~
   * @document propsfile
   * @section dt_numcontainers
   * @after header
   * - dtest.core.dockertest.numcontainers: Number of containers to run.  This defaults to 2 so that is runs well
   * out of the box on a laptop.  For serious applications you likely want to set this higher.  How high to set it
   * requires experimentation.
   */
  /**
   * Number of containers to run.  Defaults to 2.
   */
  public static final String CFG_DOCKERTEST_NUMCONTAINERS = "dtest.core.dockertest.numcontainers";
  private static final int CFG_DOCKERTEST_NUMCONTAINERS_DEFAULT = 2;

  /*~~
   * @document propsfile
   * @section dt_resultlocation
   * @after dt_numcontainers
   * - dtest.core.dockertest.resultlocation:  Directory on the build machine where results of the run will
   * be written.  Defaults to `java.io.tmpdir`.
   */
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
   * Setup the configuration.  Call this or {@link #buildConfig(String, Properties, boolean)} first.  This sets
   * cleanupAfter to true.
   * @param confDir directory with dtest.properties and dtest.yaml configurations file in it.
   * @param override properties that take precedence over values in dtest.properties file.
   * @throws IOException if the config file cannot be read.
   */
  public void buildConfig(String confDir, Properties override) throws IOException {
    buildConfig(confDir, override, true);
  }

  /**
   * Setup the configuration.  This or {@link #buildConfig(String, Properties)} should be called before other
   * methods in this class.
   * @param confDir directory with dtest.properties and dtest.yaml configuration files in it.
   * @param override properties that take precedence over values in dtest.properties.
   * @param cleanupAfter whether to cleanup after the build.  Usually you want this to be true to avoid leaving
   *                     docker images and containers lying around.  But setting it to false can be useful if you
   *                     are seeing unexpected errors in your run and want to debug them.
   * @throws IOException if the config file cannot be read.
   */
  public void buildConfig(String confDir, Properties override, boolean cleanupAfter) throws IOException {
    this.cleanupAfter = cleanupAfter;
    cfgDir = confDir; // If you came from the outside and not main this won't be set yet.
    cfg = new Config(confDir, override);
  }

  /**
   * Set the logger implementation for this instance.  This must be called before {@link #runBuild()}.
   * @param log logger implementation.
   */
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
   * Run the build.  This may take a while (obviously) and will launch a number of threads.  You must call one of
   * the buildConfig methods and {@link #setLogger(DTestLogger)} before calling this.
   * @return BuildState indicating the status of the build.
   */
  public BuildState runBuild() {
    BuildState state = null;
    boolean mightHaveBuiltImage = false;
    try {
      CodeSource codeSource = CodeSource.getInstance(cfg, log);
      buildInfo = new BuildInfo(cfgDir, codeSource, cleanupAfter);
      buildInfo.setConfig(cfg).setLog(log);
      docker = ContainerClient.getInstance(cfg, log);
      docker.setBuildInfo(buildInfo);
      ContainerCommandFactory cmdFactory = ContainerCommandFactory.getInstance(cfg, log);
      mightHaveBuiltImage = true;
      docker.buildImage(cmdFactory);
      state = runContainers(cmdFactory);
      packageLogs();
      return state;
    } catch (IOException e) {
      log.error("Failed to run the build", e);
      // we might have failed before state got set
      if (state == null) state = new BuildState();
      state.fail();
      return state;
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
        analyzer.analyzeLog(result, buildInfo.getYaml());
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
        if (buildInfo.shouldCleanupAfter()) docker.removeContainer(result);
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

  private void packageLogs() throws IOException {
    ProcessResults res = Utils.runProcess("tar", 60, log, "tar", "zcf",
        getResultsDir() + buildInfo.getLabel() + ".tgz", "-C", buildInfo.getBaseDir(), buildInfo.getLabel());
    if (res.rc != 0) {
      throw new IOException("Failed to tar up logs, error " + res.rc + " msg: " + res.stderr);
    }
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

  /*~~
   * @document dockertest
   * @section body
   * # DockerTest
   * DockerTest is a tool that allows users to run their tests in containers.  The intended users are large projects
   * with many tests that take more than a few minutes to run.  Using this tool a build machine can compile the project
   * once and then run tests in containers in parallel.  Currently the implementation is tied to Docker, Maven, and
   * Git, though effort has gone into making dtest plugable so that other container, build, and VCS tools can be used.
   *
   * ## Overview
   * DockerTest begins by building a docker image of the project.  This includes any packages the project requires,
   * as well as checking out the code and building it.  The project is built as part of the image so that each
   * container running tests starts with the code built and only needs to run the tests.
   *
   * Where to check out code from and which branch to use is controlled by the configuration files (see below).  The
   * user can also override this (and other configuration values) on the command line.  This makes it easy to use
   * dtest with build tools such as Jenkins for pre-commit builds.  The contributor can provide his or her git repo and
   * branch and then dtest can run all the tests before the code is committed.
   *
   * Each build it labeled.  The user can pass an explicit label or a random one an be generated by the system.  This
   * label is used to force a fresh image build on every run.  An image can be reused for a test run by setting the
   * system to not clean up used images and setting the label to a previous label.
   *
   * Once the image has been built a number of containers are spun up to run the tests.  The simplest configuration is
   * to have one container per directory.  Directories that take significantly longer than others can be split into
   * multiple containers.  Tests that need to be run alone can be isolated in their own containers.  Bad tests
   * can be skipped.  All of this is controlled by the [dtest.yaml](./yamlfile.html).
   *
   * Once all the containers are finished the logs from any tests that have failed or returned errors are collected
   * and returned to the user.
   *
   * dtest has five possible return states:
   * - Success:  All the tests were run, and all passed.
   * - Had failures or errors:  All the tests were run, some failed or returned errors.
   * - Had timeouts:  Some of the tests timed out.  This state overrides the previous, so some tests may also have
   * failed or returned errors.
   * - Build failed: dtest failed to complete.  This can be caused by the image failing to build or problems running
   * the containers.
   * - Build timed out:  Either the image build or one of the containers failed to finish in the allotted time.
   * This indicates a timeout in the build itself or the containers, not in individual tests.
   *
   * ## Usage
   * DockerTest can be run as a command line tool, `dtest` or as a maven plugin `dtest-maven-plugin`.  The functionality
   * is the same for each, but configuration and logging are done differently.  See [Maven Plugin](../dtest-maven-plugin/plugin.html)
   * for details on the plugin.  The `dtest` command line tool is described here.
   *
   * Command line users should define an environment variable `DTEST_HOME` that describes where the tool is located.
   * Under this directory there is a `bin` directory that controls the `dtest` executable, a `lib` directory with all
   * of the required jars, a `conf` directory, and `logs`.
   *
   * `dtest` is controlled by two configuration files.  [dtest.properties](./propsfile.html) contains general
   * information for a given instance of dtest, such as which repository to use by default, how many containers to
   * spawn simultaneously, etc.  Properties can be overridden on the command line.
   *
   * [dtest.yaml](./yamlfile.html) contains specific information for a build such as what packages it requires and
   * what directories to run tests in.
   *
   * It is assumed that different builds will have different configuration files.  Even between branches of a project
   * there will often be different versions of the configuration files since which tests to run and which to skip
   * can change.  For this reason `dtest` does not look in the `conf` directory for its configuration files but rather
   * requires the user to pass the conf directory location as part of the command line.
   *
   * The command line takes the following arguments:
   * - `-c` *conf_directory* The configuration directory that contains the `dtest.properties` and `dtest.yaml` files
   * for this build.  This is required.
   * - `-D` *property_name=propertyvalue* Configuration properties for this build.  Any value passed here overrides
   * values in `dtest.properties`.  This argument is optional.  It can be passed as many times as desired.
   *
   * Logging is handled by Log4j.  The logging configuration is in `conf/log4j2.xml`.  Logs are written to
   * `logs/dtest.log`.
   *
   * command line options (including property overrides)
   */
}
