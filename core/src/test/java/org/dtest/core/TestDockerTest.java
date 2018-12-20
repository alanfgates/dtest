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

import org.apache.commons.lang3.StringUtils;
import org.dtest.core.docker.DockerContainerClient;
import org.dtest.core.impl.ProcessResults;
import org.dtest.core.mvn.MavenResultAnalyzer;
import org.dtest.core.mvn.TestMavenResultAnalyzer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TestDockerTest {
  private static boolean imageBuilt;
  private static int succeeded;
  private static List<String> failures;
  private static List<String> errors;
  private static String logToReturn;

  public static class SuccessfulClient extends ContainerClient {

    @Override
    public String getContainerBaseDir() {
      return null;
    }

    @Override
    public void buildImage(ContainerCommandFactory cmdFactory) throws IOException {
      buildInfo.getBuildDir();
      imageBuilt = true;
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd) throws
        IOException {
      String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") + logToReturn;
          //TestMavenResultAnalyzer.LOG_SUCCESSFUL_RUN_FAILED_TESTS;
      return new ContainerResult(cmd, 0, logs);
    }

    @Override
    public void copyLogFiles(ContainerResult result, String targetDir) throws IOException {

    }

    @Override
    public void removeContainer(ContainerResult result) throws IOException {

    }

    @Override
    public void removeImage() throws IOException {

    }

    @Override
    public String getProjectName() {
      return null;
    }
  }

  /*
  TODO - Move to Hive specific tests
  public static class SuccessfulWithFailingTestsClientFactory extends ContainerClientFactory {
    @Override
    public ContainerClient getClient(BuildInfo info) {
      return new ContainerClient() {
        @Override
        public void defineImage(String dir, String repo, String branch, String label) throws IOException {

        }

        @Override
        public String getContainerBaseDir() {
          return null;
        }

        @Override
        public void buildImage(String dir, long toWait, DTestLogger logger) throws IOException {
          imageBuilt = true;
        }

        @Override
        public ContainerResult runContainer(long toWait, ContainerCommand cmd, DTestLogger logger) throws
            IOException {
          String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
              TestMavenResultAnalyzer.LOG2;
          return new ContainerResult(cmd, 0, logs);
        }
      };
    }
  }
  */

  public static class TimingOutClient extends ContainerClient {
    @Override
    public String getContainerBaseDir() {
      return null;
    }

    @Override
    public void buildImage(ContainerCommandFactory cmdFactory) throws IOException {
      buildInfo.getBuildDir();
      imageBuilt = true;
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd) throws
        IOException {
      String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
          TestMavenResultAnalyzer.LOG_TIMED_OUT;
      return new ContainerResult(cmd, 0, logs);
    }

    @Override
    public void copyLogFiles(ContainerResult result, String targetDir) throws IOException {

    }

    @Override
    public void removeContainer(ContainerResult result) throws IOException {

    }

    @Override
    public void removeImage() throws IOException {

    }

    @Override
    public String getProjectName() {
      return null;
    }
  }

  public static class FailingClient extends ContainerClient {
    @Override
    public String getContainerBaseDir() {
      return null;
    }

    @Override
    public void buildImage(ContainerCommandFactory cmdFactory) throws IOException {
      buildInfo.getBuildDir();
      imageBuilt = true;
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd) throws
        IOException {
      String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
          TestMavenResultAnalyzer.LOG_SUCCESSFUL_RUN_FAILED_TESTS;
      return new ContainerResult(cmd, 130, logs);
    }

    @Override
    public void copyLogFiles(ContainerResult result, String targetDir) throws IOException {

    }

    @Override
    public void removeContainer(ContainerResult result) throws IOException {

    }

    @Override
    public void removeImage() throws IOException {

    }

    @Override
    public String getProjectName() {
      return null;
    }
  }


  public static class HelloWorldCommandList extends ContainerCommandFactory {
    @Override
    public void buildContainerCommands(ContainerClient containerClient, BuildInfo label) throws IOException {
      ContainerCommand cmd = new ContainerCommand() {
        @Override
        public String containerSuffix() {
          return "friendly";
        }

        @Override
        public String[] shellCommand() {
          return new String[] {"echo", "hello", "world"};
        }

        @Override
        public String containerDirectory() {
          return "/tmp";
        }
      };
      cmd.setConfig(cfg).setLog(log);
      cmds.add(cmd);
    }

    @Override
    public List<String> getInitialBuildCommand() {
      return null;
    }

    @Override
    public List<String> getRequiredPackages() {
      return null;
    }
  }

  public static class SpyingResultAnalyzer extends ResultAnalyzer {
    MavenResultAnalyzer contained = new MavenResultAnalyzer();

    @Override
    public Configurable setConfig(Config cfg) {
      super.setConfig(cfg);
      contained.setConfig(cfg);
      return this;
    }

    @Override
    public Configurable setLog(DTestLogger log) {
      super.setLog(log);
      contained.setLog(log);
      return this;
    }

    @Override
    public void analyzeLog(ContainerResult result) {
      contained.analyzeLog(result);
      buildState = contained.buildState;
    }

    @Override
    public int getSucceeded() {
      succeeded = contained.getSucceeded();
      return succeeded;
    }

    @Override
    public List<String> getFailed() {
      failures = contained.getFailed();
      return failures;
    }

    @Override
    public List<String> getErrors() {
      errors = contained.getErrors();
      return errors;
    }
  }

  @Before
  public void setup() {
    imageBuilt = false;
    succeeded = 0;
    failures = new ArrayList<>();
    errors = new ArrayList<>();
  }

  @Test
  public void successfulRunAllTestsPass() {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Properties props = TestUtils.buildProperties(
        ContainerClient.CFG_CONTAINERCLIENT_IMPL, SuccessfulClient.class.getName(),
        ContainerCommandFactory.CFG_CONTAINERCOMMANDLIST_IMPL, HelloWorldCommandList.class.getName(),
        ResultAnalyzer.CFG_RESULTANALYZER_IMPL, SpyingResultAnalyzer.class.getName(),
        CodeSource.CFG_CODESOURCE_BRANCH, "successful",

        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildInfo.CFG_BUILDINFO_LABEL, "firstTry");
    logToReturn = TestMavenResultAnalyzer.LOG_SUCCESSFUL_RUN_ALL_SUCCEEDED;
    DockerTest test = new DockerTest();
    test.buildConfig(props);
    test.setLogger(log);
    BuildState state = test.runBuild();
    log.dumpToLog();
    Assert.assertEquals(BuildState.State.SUCCEEDED, state.getState());
    Assert.assertTrue(imageBuilt);
    Assert.assertEquals(0, errors.size());
    Assert.assertEquals(0, failures.size());
    Assert.assertEquals(19, succeeded);
    Assert.assertTrue(log.toString().contains("SUCCEEDED, the build ran to completion and all tests passed"));
  }

  @Test
  public void successfulRunSomeTestsFail() {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Properties props = TestUtils.buildProperties(
        ContainerClient.CFG_CONTAINERCLIENT_IMPL, SuccessfulClient.class.getName(),
        ContainerCommandFactory.CFG_CONTAINERCOMMANDLIST_IMPL, HelloWorldCommandList.class.getName(),
        ResultAnalyzer.CFG_RESULTANALYZER_IMPL, SpyingResultAnalyzer.class.getName(),
        CodeSource.CFG_CODESOURCE_BRANCH, "successful",

        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildInfo.CFG_BUILDINFO_LABEL, "firstTry");
    logToReturn = TestMavenResultAnalyzer.LOG_SUCCESSFUL_RUN_FAILED_TESTS;
    DockerTest test = new DockerTest();
    test.buildConfig(props);
    test.setLogger(log);
    BuildState state = test.runBuild();
    log.dumpToLog();
    Assert.assertEquals(BuildState.State.HAD_FAILURES_OR_ERRORS, state.getState());
    Assert.assertTrue(imageBuilt);
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("TestAcidOnTez.testGetSplitsLocks", errors.get(0));
    Assert.assertEquals(1, failures.size());
    Assert.assertEquals("TestActivePassiveHA.testManualFailover", failures.get(0));
    Assert.assertEquals(32, succeeded);
    Assert.assertTrue(log.toString().contains("HAD FAILURES OR ERRORS, the build ran to completion but some tests failed or errored out"));
  }

  /*
  @Test
  public void successfulRunSomeTestsFail() {
    Config.CONTAINER_CLIENT_FACTORY.set(SuccessfulWithFailingTestsClientFactory.class.getName());
    Config.CONTAINER_COMMAND_FACTORY.set(ItestCommandFactory.class.getName());
    Config.RESULT_ANALYZER_FACTORY.set(SpyingResultAnalyzerFactory.class.getName());
    DockerTest test = new DockerTest(out, err);
    BuildInfo build = test.parseArgs(new String[] {"-b", "successful",
                                                   "-d", System.getProperty("java.io.tmpdir"),
                                                   "-l", "secondTry",
                                                   "-r", "repo",
                                                   "-p", "profile1"});
    test.runBuild(build);
    Assert.assertTrue(imageBuilt);
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("TestNegativeCliDriver.alter_notnull_constraint_violation", errors.get(0));
    Assert.assertEquals(1, failures.size());
    Assert.assertEquals("TestNegativeCliDriver.alter_table_constraint_duplicate_pk", failures.get(0));
    Assert.assertEquals(72, succeeded);
    Assert.assertFalse(hadTimeouts);
    Assert.assertTrue(runSucceeded);
    Assert.assertTrue(outBuffer.toString().contains("Test run SUCCEEDED"));
  }
  */

  @Test
  public void timeout() {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Properties props = TestUtils.buildProperties(
        ContainerClient.CFG_CONTAINERCLIENT_IMPL, TimingOutClient.class.getName(),
        ContainerCommandFactory.CFG_CONTAINERCOMMANDLIST_IMPL, HelloWorldCommandList.class.getName(),
        ResultAnalyzer.CFG_RESULTANALYZER_IMPL, SpyingResultAnalyzer.class.getName(),
        CodeSource.CFG_CODESOURCE_BRANCH, "failure",
        CodeSource.CFG_CODESOURCE_REPO, "repo",
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildInfo.CFG_BUILDINFO_LABEL, "will-time-out");
    DockerTest test = new DockerTest();
    test.buildConfig(props);
    test.setLogger(log);
    BuildState state = test.runBuild();
    log.dumpToLog();
    Assert.assertEquals(BuildState.State.HAD_TIMEOUTS, state.getState());
    Assert.assertTrue(imageBuilt);
    Assert.assertTrue(log.toString().contains("HAD TIMEOUTS, the build ran to completion but some containers timed out"));
  }

  @Test
  public void failedRun() {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Properties props = TestUtils.buildProperties(
        ContainerClient.CFG_CONTAINERCLIENT_IMPL, FailingClient.class.getName(),
        ContainerCommandFactory.CFG_CONTAINERCOMMANDLIST_IMPL, HelloWorldCommandList.class.getName(),
        ResultAnalyzer.CFG_RESULTANALYZER_IMPL, SpyingResultAnalyzer.class.getName(),
        CodeSource.CFG_CODESOURCE_BRANCH, "failure",
        CodeSource.CFG_CODESOURCE_REPO, "repo",
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildInfo.CFG_BUILDINFO_LABEL, "take2");
    DockerTest test = new DockerTest();
    test.buildConfig(props);
    test.setLogger(log);
    BuildState state = test.runBuild();
    log.dumpToLog();
    Assert.assertEquals(BuildState.State.FAILED, state.getState());
    Assert.assertTrue(imageBuilt);
    Assert.assertTrue(log.toString().contains("FAILED, the build did not run to completion"));
  }

  @Test
  public void successfulImageBuild() throws IOException {
    DockerContainerClient.checkBuildSucceeded(new ProcessResults(
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Packaging .................................... SUCCESS [1.924s]\n" +
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD SUCCESS\n" +
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 9:45.700s\n" +
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 18:19:45 UTC 2018\n" +
        "2018-04-04T11:19:46,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 514M/11950M\n" +
        "2018-04-04T11:19:46,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
        "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - QFile Accumulo Tests ........... SUCCESS [6.899s]\n" +
        "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] JMH benchmark: Hive ............................... SUCCESS [21.935s]\n" +
        "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests - Hadoop 2 .......... SUCCESS [3.726s]\n" +
        "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests with miniKdc ........ SUCCESS [4.960s]\n" +
        "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
        "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD SUCCESS\n" +
        "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
        "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 2:07.203s\n" +
        "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 18:21:54 UTC 2018\n" +
        "2018-04-04T11:21:55,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 493M/4670M\n" +
        "2018-04-04T11:21:55,982  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n", "", 0));
  }

  @Test(expected = IOException.class)
  public void imageBuildSucceededButBuildFailed() throws IOException {
    DockerContainerClient.checkBuildSucceeded(new ProcessResults(
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Packaging .................................... SUCCESS [1.924s]\n" +
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD SUCCESS\n" +
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 9:45.700s\n" +
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 18:19:45 UTC 2018\n" +
        "2018-04-04T11:19:46,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 514M/11950M\n" +
        "2018-04-04T11:19:46,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Custom UDFs - udf-classloader-udf2  SUCCESS [0.780s]\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Custom UDFs - udf-vectorized-badexample  SUCCESS [0.896s]\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - HCatalog Unit Tests ............ FAILURE [1:44.582s]\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - QFile Druid Tests .............. SKIPPED\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Testing Utilities .............. SKIPPED\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests ..................... SKIPPED\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Blobstore Tests ................ SKIPPED\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Test Serde ..................... SKIPPED\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - QFile Tests .................... SKIPPED\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - QFile Accumulo Tests ........... SKIPPED\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] JMH benchmark: Hive ............................... SKIPPED\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests - Hadoop 2 .......... SKIPPED\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests with miniKdc ........ SKIPPED\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD FAILURE\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 2:22.569s\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 20:39:58 UTC 2018\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 83M/3436M\n" +
        "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n", "", 0));
  }

  @Test(expected = IOException.class)
  public void imageBuildSuccessfulButBothBuildsFailed() throws IOException {
    DockerContainerClient.checkBuildSucceeded(new ProcessResults(
        "2018-04-04T13:37:34,371  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive TestUtils .................................... SKIPPED\n" +
            "2018-04-04T13:37:34,371  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Packaging .................................... SKIPPED\n" +
            "2018-04-04T13:37:34,371  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T13:37:34,371  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD FAILURE\n" +
            "2018-04-04T13:37:34,371  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T13:37:34,371  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 2:47.572s\n" +
            "2018-04-04T13:37:34,371  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 20:37:33 UTC 2018\n" +
            "2018-04-04T13:37:35,371  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 138M/2701M\n" +
            "2018-04-04T13:37:35,371  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests - Hadoop 2 .......... SKIPPED\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests with miniKdc ........ SKIPPED\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD FAILURE\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 2:22.569s\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 20:39:58 UTC 2018\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 83M/3436M\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n", "", 0));
  }

  @Test(expected = IOException.class)
  public void imageBuildFailed() throws IOException {
    DockerContainerClient.checkBuildSucceeded(new ProcessResults(
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Packaging .................................... SUCCESS [1.924s]\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD SUCCESS\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 9:45.700s\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 18:19:45 UTC 2018\n" +
            "2018-04-04T11:19:46,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 514M/11950M\n" +
            "2018-04-04T11:19:46,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - QFile Accumulo Tests ........... SUCCESS [6.899s]\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] JMH benchmark: Hive ............................... SUCCESS [21.935s]\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests - Hadoop 2 .......... SUCCESS [3.726s]\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests with miniKdc ........ SUCCESS [4.960s]\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD SUCCESS\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 2:07.203s\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 18:21:54 UTC 2018\n" +
            "2018-04-04T11:21:55,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 493M/4670M\n" +
            "2018-04-04T11:21:55,982  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n", "", 1));
  }
}
