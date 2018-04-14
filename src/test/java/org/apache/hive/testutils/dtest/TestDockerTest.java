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

import org.apache.commons.lang3.StringUtils;
import org.apache.hive.testutils.dtest.impl.ContainerResult;
import org.apache.hive.testutils.dtest.impl.DTestLogger;
import org.apache.hive.testutils.dtest.impl.DockerClient;
import org.apache.hive.testutils.dtest.impl.ProcessResults;
import org.apache.hive.testutils.dtest.impl.SimpleResultAnalyzer;
import org.apache.hive.testutils.dtest.impl.TestSimpleResultAnalyzer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestDockerTest {
  private static final Logger LOG = LoggerFactory.getLogger(TestDockerTest.class);
  private static boolean imageBuilt, hadTimeouts, runSucceeded;
  private static int succeeded;
  private static List<String> failures;
  private static List<String> errors;

  private ByteArrayOutputStream outBuffer;
  private PrintStream out;
  private PrintStream err;

  public static class SuccessfulClientFactory extends ContainerClientFactory {
    @Override
    public ContainerClient getClient(String label) {
      return new ContainerClient() {
        @Override
        public void buildImage(String dir, long toWait, TimeUnit unit, DTestLogger logger) throws IOException {
          imageBuilt = true;
        }

        @Override
        public ContainerResult runContainer(long toWait, TimeUnit unit, ContainerCommand cmd, DTestLogger logger) throws
            IOException {
          String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
              TestSimpleResultAnalyzer.LOG1;
          return new ContainerResult(cmd.containerName(), 0, logs);
        }
      };
    }
  }

  public static class SuccessfulWithFailingTestsClientFactory extends ContainerClientFactory {
    @Override
    public ContainerClient getClient(String label) {
      return new ContainerClient() {
        @Override
        public void buildImage(String dir, long toWait, TimeUnit unit, DTestLogger logger) throws IOException {
          imageBuilt = true;
        }

        @Override
        public ContainerResult runContainer(long toWait, TimeUnit unit, ContainerCommand cmd, DTestLogger logger) throws
            IOException {
          String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
              TestSimpleResultAnalyzer.LOG2;
          return new ContainerResult(cmd.containerName(), 0, logs);
        }
      };
    }
  }

  public static class TimingOutClientFactory extends ContainerClientFactory {
    @Override
    public ContainerClient getClient(String label) {
      return new ContainerClient() {
        @Override
        public void buildImage(String dir, long toWait, TimeUnit unit, DTestLogger logger) throws IOException {
          imageBuilt = true;
        }

        @Override
        public ContainerResult runContainer(long toWait, TimeUnit unit, ContainerCommand cmd, DTestLogger logger) throws
            IOException {
          String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
              TestSimpleResultAnalyzer.LOG3;
          return new ContainerResult(cmd.containerName(), 0, logs);
        }
      };
    }
  }

  public static class FailingClientFactory extends ContainerClientFactory {
    @Override
    public ContainerClient getClient(String label) {
      return new ContainerClient() {
        @Override
        public void buildImage(String dir, long toWait, TimeUnit unit, DTestLogger logger) throws IOException {
          imageBuilt = true;
        }

        @Override
        public ContainerResult runContainer(long toWait, TimeUnit unit, ContainerCommand cmd, DTestLogger logger) throws
            IOException {
          String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
              TestSimpleResultAnalyzer.LOG1;
          return new ContainerResult(cmd.containerName(), 130, logs);
        }
      };
    }
  }


  public static class HelloWorldCommandFactory extends ContainerCommandFactory {
    @Override
    public List<ContainerCommand> getContainerCommands(String baseDir) throws IOException {
      return Collections.singletonList(new ContainerCommand() {
        @Override
        public String containerName() {
          return "friendly";
        }

        @Override
        public String[] shellCommand() {
          return new String[] {"echo", "hello", "world"};
        }
      });
    }
  }

  public static class ItestCommandFactory extends ContainerCommandFactory {
    @Override
    public List<ContainerCommand> getContainerCommands(String baseDir) throws IOException {
      return Collections.singletonList(new ContainerCommand() {
        @Override
        public String containerName() {
          return "friendly-itests-qtest";
        }

        @Override
        public String[] shellCommand() {
          return new String[] {"echo", "hello", "world"};
        }
      });
    }
  }

  public static class SpyingResultAnalyzerFactory extends ResultAnalyzerFactory {
    @Override
    public ResultAnalyzer getAnalyzer() {
      final SimpleResultAnalyzer contained = new SimpleResultAnalyzer();
      return new ResultAnalyzer() {
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
      };
    }
  }

  @Before
  public void setup() {
    imageBuilt = false;
    succeeded = 0;
    failures = new ArrayList<>();
    errors = new ArrayList<>();
    outBuffer = new ByteArrayOutputStream();
    out = new PrintStream(outBuffer);
    err = new PrintStream(new ByteArrayOutputStream());
  }

  @Test
  public void successfulRunAllTestsPass() {
    System.setProperty(ContainerClientFactory.PROPERTY, SuccessfulClientFactory.class.getName());
    System.setProperty(ContainerCommandFactory.PROPERTY, HelloWorldCommandFactory.class.getName());
    System.setProperty(ResultAnalyzerFactory.PROPERTY, SpyingResultAnalyzerFactory.class.getName());
    DockerTest test = new DockerTest(out, err);
    test.parseArgs(new String[] {"-b", "successful",
                                 "-d", System.getProperty("java.io.tmpdir"),
                                 "-l", "firstTry",
                                 "-r", "repo"});
    test.startBuild(test.singleBuild);
    Assert.assertTrue(imageBuilt);
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("TestAcidOnTez.testGetSplitsLocks", errors.get(0));
    Assert.assertEquals(1, failures.size());
    Assert.assertEquals("TestActivePassiveHA.testManualFailover", failures.get(0));
    Assert.assertEquals(32, succeeded);
    Assert.assertFalse(hadTimeouts);
    Assert.assertTrue(runSucceeded);
    Assert.assertTrue(outBuffer.toString().contains("Test run RAN ALL TESTS"));
  }

  @Test
  public void successfulRunSomeTestsFail() {
    System.setProperty(ContainerClientFactory.PROPERTY, SuccessfulWithFailingTestsClientFactory.class.getName());
    System.setProperty(ContainerCommandFactory.PROPERTY, ItestCommandFactory.class.getName());
    System.setProperty(ResultAnalyzerFactory.PROPERTY, SpyingResultAnalyzerFactory.class.getName());
    DockerTest test = new DockerTest(out, err);
    test.parseArgs(new String[] {"-b", "successful",
                                 "-d", System.getProperty("java.io.tmpdir"),
                                 "-l", "secondTry",
                                 "-r", "repo"});
    test.startBuild(test.singleBuild);
    Assert.assertTrue(imageBuilt);
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("TestNegativeCliDriver.alter_notnull_constraint_violation", errors.get(0));
    Assert.assertEquals(1, failures.size());
    Assert.assertEquals("TestNegativeCliDriver.alter_table_constraint_duplicate_pk", failures.get(0));
    Assert.assertEquals(72, succeeded);
    Assert.assertFalse(hadTimeouts);
    Assert.assertTrue(runSucceeded);
    Assert.assertTrue(outBuffer.toString().contains("Test run RAN ALL TESTS"));
  }

  @Test
  public void timeout() {
    System.setProperty(ContainerClientFactory.PROPERTY, TimingOutClientFactory.class.getName());
    System.setProperty(ContainerCommandFactory.PROPERTY, HelloWorldCommandFactory.class.getName());
    System.setProperty(ResultAnalyzerFactory.PROPERTY, SpyingResultAnalyzerFactory.class.getName());
    DockerTest test = new DockerTest(out, err);
    test.parseArgs(new String[] {"-b", "failure",
                                 "-d", System.getProperty("java.io.tmpdir"),
                                 "-l", "will-timeout",
                                 "-r", "repo"});
    test.startBuild(test.singleBuild);
    Assert.assertTrue(imageBuilt);
    Assert.assertTrue(hadTimeouts);
    Assert.assertTrue(runSucceeded);
    Assert.assertTrue(outBuffer.toString().contains("Test run HAD TIMEOUTS.  Following numbers are incomplete."));
  }

  @Test
  public void failedRun() {
    System.setProperty(ContainerClientFactory.PROPERTY, FailingClientFactory.class.getName());
    System.setProperty(ContainerCommandFactory.PROPERTY, HelloWorldCommandFactory.class.getName());
    System.setProperty(ResultAnalyzerFactory.PROPERTY, SpyingResultAnalyzerFactory.class.getName());
    DockerTest test = new DockerTest(out, err);
    test.parseArgs(new String[] {"-b", "failure",
                                 "-d", System.getProperty("java.io.tmpdir"),
                                 "-l", "take2",
                                 "-r", "repo"});
    test.startBuild(test.singleBuild);
    Assert.assertTrue(imageBuilt);
    Assert.assertFalse(hadTimeouts);
    Assert.assertFalse(runSucceeded);
    Assert.assertTrue(outBuffer.toString().contains("Test run FAILED.  Following numbers are probably meaningless."));
  }

  @Test
  public void successfulImageBuild() throws IOException {
    DockerClient.checkBuildSucceeded(new ProcessResults(
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
    DockerClient.checkBuildSucceeded(new ProcessResults(
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
    DockerClient.checkBuildSucceeded(new ProcessResults(
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
    DockerClient.checkBuildSucceeded(new ProcessResults(
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
