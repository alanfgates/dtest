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
import org.dtest.core.git.GitSource;
import org.dtest.core.impl.ProcessResults;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class TestDockerTest {
  private static final Logger LOG = LoggerFactory.getLogger(TestDockerTest.class);
  private static boolean imageBuilt, hadTimeouts, runSucceeded;
  private static int succeeded;
  private static List<String> failures;
  private static List<String> errors;

  private ByteArrayOutputStream outBuffer;
  private PrintStream out;
  private PrintStream err;

  public static class SuccessfulClient extends ContainerClient {
    @Override
    public void defineImage() throws IOException {

    }

    @Override
    public String getContainerBaseDir() {
      return null;
    }

    @Override
    public void buildImage(String dir, DTestLogger logger) throws IOException {
      imageBuilt = true;
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd, DTestLogger logger) throws
        IOException {
      String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
          TestBaseResultAnalyzer.LOG1;
      return new ContainerResult(cmd, 0, logs);
    }

    @Override
    public void copyLogFiles(ContainerResult result, String targetDir, DTestLogger logger) throws IOException {

    }

    @Override
    public void removeContainer(ContainerResult result, DTestLogger logger) throws IOException {

    }

    @Override
    public void removeImage(DTestLogger logger) throws IOException {

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
              TestBaseResultAnalyzer.LOG2;
          return new ContainerResult(cmd, 0, logs);
        }
      };
    }
  }
  */

  public static class TimingOutClient extends ContainerClient {
    @Override
    public void defineImage() throws IOException {

    }

    @Override
    public String getContainerBaseDir() {
      return null;
    }

    @Override
    public void buildImage(String dir, DTestLogger logger) throws IOException {
      imageBuilt = true;
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd, DTestLogger logger) throws
        IOException {
      String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
          TestBaseResultAnalyzer.LOG3;
      return new ContainerResult(cmd, 0, logs);
    }

    @Override
    public void copyLogFiles(ContainerResult result, String targetDir, DTestLogger logger) throws IOException {

    }

    @Override
    public void removeContainer(ContainerResult result, DTestLogger logger) throws IOException {

    }

    @Override
    public void removeImage(DTestLogger logger) throws IOException {

    }

    @Override
    public String getProjectName() {
      return null;
    }
  }

  public static class FailingClient extends ContainerClient {
    @Override
    public void defineImage() throws IOException {

    }

    @Override
    public String getContainerBaseDir() {
      return null;
    }

    @Override
    public void buildImage(String dir, DTestLogger logger) throws IOException {
      imageBuilt = true;
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd, DTestLogger logger) throws
        IOException {
      String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
          TestBaseResultAnalyzer.LOG1;
      return new ContainerResult(cmd, 130, logs);
    }

    @Override
    public void copyLogFiles(ContainerResult result, String targetDir, DTestLogger logger) throws IOException {

    }

    @Override
    public void removeContainer(ContainerResult result, DTestLogger logger) throws IOException {

    }

    @Override
    public void removeImage(DTestLogger logger) throws IOException {

    }

    @Override
    public String getProjectName() {
      return null;
    }
  }


  public static class HelloWorldCommandList extends ContainerCommandList {
    @Override
    public void buildContainerCommands(ContainerClient containerClient, BuildInfo label,
                                       DTestLogger logger) throws IOException {
      add(new ContainerCommand() {
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
      });
    }
  }

  public static class ItestCommandList extends ContainerCommandList {
    @Override
    public void buildContainerCommands(ContainerClient containerClient, BuildInfo label,
                                       DTestLogger logger) throws IOException {
      add(new ContainerCommand() {
        @Override
        public String containerSuffix() {
          return "friendly-itests-qtest";
        }

        @Override
        public String[] shellCommand() {
          return new String[] {"echo", "hello", "world"};
        }

        @Override
        public String containerDirectory() {
          return "/tmp";
        }
      });
    }
  }

  public static class SpyingResultAnalyzer extends ResultAnalyzer {
    BaseResultAnalyzer contained = new BaseResultAnalyzer();
    @Override
    public void analyzeLog(ContainerResult result) {
      contained.analyzeLog(result);
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

    @Override
    public boolean hadTimeouts() {
      hadTimeouts = contained.hadTimeouts();
      return hadTimeouts;
    }

    @Override
    public boolean runSucceeded() {
      runSucceeded = contained.runSucceeded();
      return runSucceeded;
    }
  }

  @Before
  public void setup() {
    imageBuilt = false;
    succeeded = 0;
    failures = new ArrayList<>();
    errors = new ArrayList<>();
    hadTimeouts = false;
    outBuffer = new ByteArrayOutputStream();
    out = new PrintStream(outBuffer);
    err = new PrintStream(new ByteArrayOutputStream());
  }

  @Test
  public void successfulRunAllTestsPass() {
    Config.set(ContainerClient.CFG_CONTAINER_CLIENT, SuccessfulClient.class.getName());
    Config.set(ContainerCommandList.CFG_CONTAINER_COMMAND_LIST, HelloWorldCommandList.class.getName());
    Config.set(ResultAnalyzer.CFG_RESULT_ANALYZER, SpyingResultAnalyzer.class.getName());
    Config.set(GitSource.CFG_GIT_BRANCH, "successful");
    Config.set(GitSource.CFG_GIT_REPO, "repo");
    Config.set(BuildInfo.CFG_BUILD_BASE_DIR, System.getProperty("java.io.tmpdir"));
    DockerTest test = new DockerTest(out, err);
    BuildInfo build = test.parseArgs(new String[] {"-l", "firstTry",
                                                   "-p", "profile1"});
    test.runBuild(build);
    Assert.assertTrue(imageBuilt);
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("TestAcidOnTez.testGetSplitsLocks", errors.get(0));
    Assert.assertEquals(1, failures.size());
    Assert.assertEquals("TestActivePassiveHA.testManualFailover", failures.get(0));
    Assert.assertEquals(32, succeeded);
    Assert.assertFalse(hadTimeouts);
    Assert.assertTrue(runSucceeded);
    Assert.assertTrue(outBuffer.toString().contains("Test run SUCCEEDED"));
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
    Config.set(ContainerClient.CFG_CONTAINER_CLIENT, TimingOutClient.class.getName());
    Config.set(ContainerCommandList.CFG_CONTAINER_COMMAND_LIST, HelloWorldCommandList.class.getName());
    Config.set(ResultAnalyzer.CFG_RESULT_ANALYZER, SpyingResultAnalyzer.class.getName());
    Config.set(GitSource.CFG_GIT_BRANCH, "failure");
    Config.set(GitSource.CFG_GIT_REPO, "repo");
    Config.set(BuildInfo.CFG_BUILD_BASE_DIR, System.getProperty("java.io.tmpdir"));
    DockerTest test = new DockerTest(out, err);
    BuildInfo build = test.parseArgs(new String[] {"-l", "will-time-out",
                                                   "-p", "profile1"});
    test.runBuild(build);
    Assert.assertTrue(imageBuilt);
    Assert.assertTrue(hadTimeouts);
    Assert.assertTrue(runSucceeded);
    Assert.assertTrue(outBuffer.toString().contains("Test run HAD TIMEOUTS.  Following numbers are incomplete."));
  }

  @Test
  public void failedRun() {
    Config.set(ContainerClient.CFG_CONTAINER_CLIENT, FailingClient.class.getName());
    Config.set(ContainerCommandList.CFG_CONTAINER_COMMAND_LIST, HelloWorldCommandList.class.getName());
    Config.set(ResultAnalyzer.CFG_RESULT_ANALYZER, SpyingResultAnalyzer.class.getName());
    Config.set(GitSource.CFG_GIT_BRANCH, "failure");
    Config.set(GitSource.CFG_GIT_REPO, "repo");
    Config.set(BuildInfo.CFG_BUILD_BASE_DIR, System.getProperty("java.io.tmpdir"));
    DockerTest test = new DockerTest(out, err);
    BuildInfo build = test.parseArgs(new String[] {"-l", "take2",
                                                   "-p", "profile1"});
    test.runBuild(build);
    Assert.assertTrue(imageBuilt);
    Assert.assertFalse(hadTimeouts);
    Assert.assertFalse(runSucceeded);
    Assert.assertTrue(outBuffer.toString().contains("Test run FAILED, this can mean tests failed or mvn commands failed to execute properly."));
  }

  @Test
  public void successfulImageBuild() throws IOException {
    BaseDockerClient.checkBuildSucceeded(new ProcessResults(
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
    BaseDockerClient.checkBuildSucceeded(new ProcessResults(
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
    BaseDockerClient.checkBuildSucceeded(new ProcessResults(
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
    BaseDockerClient.checkBuildSucceeded(new ProcessResults(
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
