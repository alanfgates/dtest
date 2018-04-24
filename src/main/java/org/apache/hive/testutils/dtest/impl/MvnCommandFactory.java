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
package org.apache.hive.testutils.dtest.impl;

import org.apache.hive.testutils.dtest.Config;
import org.apache.hive.testutils.dtest.ContainerClient;
import org.apache.hive.testutils.dtest.ContainerCommand;
import org.apache.hive.testutils.dtest.ContainerCommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MvnCommandFactory extends ContainerCommandFactory {
  private static final Logger LOG = LoggerFactory.getLogger(MvnCommandFactory.class);
  // Tests that we want to split out and run only parts of at a time
  private static final String[] SPECIALLY_HANDLED_TESTS = { "TestCliDriver",
      "TestMinimrCliDriver", "TestEncryptedHDFSCliDriver", "TestNegativeMinimrCliDriver",
      "TestNegativeCliDriver", "TestHBaseCliDriver", "TestMiniTezCliDriver",
      "TestMiniLlapCliDriver", "TestMiniLlapLocalCliDriver"};
  private static final Pattern DRIVER_PATTERN = Pattern.compile("Test.*Driver\\.java");
  private static final Pattern[] DIRS_TO_SKIP = {
      Pattern.compile("testutils")
  };

  private static class TestDirInfo {
    final String dir;
    final Pattern pattern;
    final Deque<String> testsToRun;
    final boolean lookForTests;

    TestDirInfo(String dir, String pattern, boolean lookForTests) {
      this.dir = dir;
      this.pattern = Pattern.compile(pattern);
      testsToRun = new ArrayDeque<>();
      this.lookForTests = lookForTests;

    }

    TestDirInfo(String dir, boolean lookForTests) {
      this(dir, dir + "/src/test", lookForTests);
    }

    TestDirInfo(String dir) {
      this(dir, true);
    }


  }

  // Set up the testing configuration
  private static final List<TestDirInfo> TEST_INFOS = new ArrayList<>();
  static {
    TEST_INFOS.add(new TestDirInfo("accumulo-handler"));
    TEST_INFOS.add(new TestDirInfo("beeline"));
    TEST_INFOS.add(new TestDirInfo("cli"));
    TEST_INFOS.add(new TestDirInfo("common"));
    TEST_INFOS.add(new TestDirInfo("contrib"));
    TEST_INFOS.add(new TestDirInfo("hplsql"));
    TEST_INFOS.add(new TestDirInfo("jdbc"));
    TEST_INFOS.add(new TestDirInfo("jdbc-handler"));
    TEST_INFOS.add(new TestDirInfo("serde"));
    TEST_INFOS.add(new TestDirInfo("shims/0.23", "shims/0.23/src/main/test", true));
    TEST_INFOS.add(new TestDirInfo("shims/common", "shims/common/src/main/test", true));
    TEST_INFOS.add(new TestDirInfo("storage-api"));
    TEST_INFOS.add(new TestDirInfo("llap-client"));
    TEST_INFOS.add(new TestDirInfo("llap-common"));
    TEST_INFOS.add(new TestDirInfo("llap-server"));
    TEST_INFOS.add(new TestDirInfo("llap-tez"));
    TEST_INFOS.add(new TestDirInfo("standalone-metastore"));
    TEST_INFOS.add(new TestDirInfo("druid-handler"));
    TEST_INFOS.add(new TestDirInfo("service"));
    TEST_INFOS.add(new TestDirInfo("spark-client"));
    TEST_INFOS.add(new TestDirInfo("streaming"));
    TEST_INFOS.add(new TestDirInfo("hbase-handler"));
    TEST_INFOS.add(new TestDirInfo("hcatalog/core"));
    TEST_INFOS.add(new TestDirInfo("hcatalog/hcatalog-pig-adapter"));
    TEST_INFOS.add(new TestDirInfo("hcatalog/server-extensions"));
    TEST_INFOS.add(new TestDirInfo("hcatalog/streaming"));
    TEST_INFOS.add(new TestDirInfo("hcatalog/webhcat/java-client"));
    TEST_INFOS.add(new TestDirInfo("hcatalog/webhcat/svr"));
    TEST_INFOS.add(new TestDirInfo("ql"));
    TEST_INFOS.add(new TestDirInfo("ql", "ql/target/generated-test-sources", true));
    TEST_INFOS.add(new TestDirInfo("itests/hcatalog-unit"));
    TEST_INFOS.add(new TestDirInfo("itests/hive-blobstore"));
    TEST_INFOS.add(new TestDirInfo("itests/hive-minikdc"));
    TEST_INFOS.add(new TestDirInfo("itests/hive-unit"));
    TEST_INFOS.add(new TestDirInfo("itests/hive-unit-hadoop2"));
    TEST_INFOS.add(new TestDirInfo("itests/test-serde", "src/main/java", true));
    TEST_INFOS.add(new TestDirInfo("itests/util"));
    TestDirInfo tid = new TestDirInfo("itests/hive-blobstore", false);
    tid.testsToRun.add("TestBlobstoreCliDriver");
    TEST_INFOS.add(tid);
    tid = new TestDirInfo("itests/hive-blobstore", false);
    tid.testsToRun.add("TestBlobstoreNegativeCliDriver");
    TEST_INFOS.add(tid);
    tid = new TestDirInfo("itests/qtest", false);
    tid.testsToRun.add("TestBeeLineDriver");
    TEST_INFOS.add(tid);
    tid = new TestDirInfo("itests/qtest", false);
    tid.testsToRun.add("TestContribCliDriver");
    TEST_INFOS.add(tid);
    tid = new TestDirInfo("itests/qtest", false);
    tid.testsToRun.add("TestContribNegativeCliDriver");
    TEST_INFOS.add(tid);
    tid = new TestDirInfo("itests/qtest", false);
    tid.testsToRun.add("TestHBaseNegativeCliDriver");
    TEST_INFOS.add(tid);
    tid = new TestDirInfo("itests/qtest", false);
    tid.testsToRun.add("TestMiniDruidCliDriver");
    TEST_INFOS.add(tid);
    tid = new TestDirInfo("itests/qtest", false);
    tid.testsToRun.add("TestMiniDruidKafkaCliDriver");
    TEST_INFOS.add(tid);
    tid = new TestDirInfo("itests/qtest", false);
    tid.testsToRun.add("TestTezPerfCliDriver");
    TEST_INFOS.add(tid);
    tid = new TestDirInfo("itests/qtest", false);
    tid.testsToRun.add("TestParseNegativeDriver");
    TEST_INFOS.add(tid);
    tid = new TestDirInfo("itests/qtest-accumulo", false);
    tid.testsToRun.add("TestAccumuloCliDriver");
    TEST_INFOS.add(tid);
  };

  @Override
  public List<ContainerCommand> getContainerCommands(final ContainerClient containerClient,
                                                     final String label,
                                                     DTestLogger logger) throws IOException {
    List<ContainerCommand> cmds = new ArrayList<>();
    String baseDir = containerClient.getContainerBaseDir();
    int testsPerContainer = Integer.valueOf(System.getProperty(Config.TESTS_PER_CONTAINER, "20"));

    // Read master-mr2.properties
    String masterPropertiesString = runContainer(containerClient, label, "read-master-m2",
        "cat testutils/ptest2/conf/deployed/master-mr2.properties", logger);
    Properties masterProperties = new Properties();
    masterProperties.load(new StringReader(masterPropertiesString));

    addUnitTests(containerClient, label, logger, baseDir, masterProperties, testsPerContainer,
        cmds);

    addSeparatedQfileTests(containerClient, label, logger, baseDir, masterProperties, testsPerContainer,
        cmds);

    return cmds;
  }

  private String runContainer(ContainerClient containerClient, final String label,
                              final String containerName,
                              final String cmd, DTestLogger logger) throws IOException {
    ContainerResult result = containerClient.runContainer(1, TimeUnit.MINUTES,
        new ContainerCommand() {
          @Override
          public String containerName() {
            return Utils.buildContainerName(label, containerName);
          }

          @Override
          public String[] shellCommand() {
            return Utils.shellCmdInRoot(containerClient.getContainerBaseDir(), () -> cmd);
          }
        }, logger);
    if (result.rc != 0) {
      String msg = "Failed to run cmd " + cmd + " as part of determining tests to run";
      LOG.error(msg);
      throw new IOException(msg);
    }
    return result.logs;
  }

  private void addUnitTests(ContainerClient containerClient, String label, DTestLogger logger,
                            String baseDir, Properties masterProperties, int testsPerContainer,
                            List<ContainerCommand> cmds) throws IOException {
    // Find all of the unit tests
    String allUnitTests = runContainer(containerClient, label, "find-all-unit-tests",
        "find . -name Test\\*\\.java", logger);

    // Determine all the unit tests.  Strain out anything that requires special handling and the
    // excludes from masterProperties
    Set<String> excludedTests = new HashSet<>();
    Collections.addAll(excludedTests, SPECIALLY_HANDLED_TESTS);
    String excludedTestsStr = masterProperties.getProperty("unitTests.exclude");
    if (excludedTestsStr != null && excludedTestsStr.length() > 0) {
      String[] excludedTestsArray = excludedTestsStr.trim().split(" ");
      Collections.addAll(excludedTests, excludedTestsArray);
    }

    int containerNumber = 1;
    for (String line : allUnitTests.split("\n")) {
      String testPath = line.trim();

      // Isolate the test name
      String[] pathElements = testPath.split(File.separator);
      String testName = pathElements[pathElements.length - 1];
      // Make sure we should be running this test
      if (excludedTests.contains(testName)) continue;

      // Strain out the Driver tests, as I've already handled them separately
      Matcher m = DRIVER_PATTERN.matcher(testName);
      if (m.matches()) continue;

      // Strain out any directories we want to skip
      boolean skip = false;
      for (Pattern p : DIRS_TO_SKIP) {
        m = p.matcher(testPath);
        if (m.find()) {
          LOG.debug("Skipping test " + testPath);
          skip = true;
        }
      }
      if (skip) continue;

      // Figure out which directory this belongs in
      boolean foundAHome = false;
      for (TestDirInfo tid : TEST_INFOS) {
        m = tid.pattern.matcher(testPath);
        if (m.find()) {
          LOG.debug("Placing test " + testPath + " into group for " + tid.dir);
          tid.testsToRun.add(testName);
          foundAHome = true;
          break;
        }
      }
      if (!foundAHome) {
        // If we got here we don't know what to do with this test
        throw new RuntimeException("Can't figure out how to handle test " + testPath);
      }
    }

    for (TestDirInfo tid : TEST_INFOS) {
      while (tid.testsToRun.size() > 0) {
        MvnCommand mvn = new MvnCommand(baseDir + File.separator + tid.dir, containerNumber++);
        for (int i = 0; i < testsPerContainer && tid.testsToRun.size() > 0; i++) {
          String oneTest = tid.testsToRun.pop();
          LOG.debug("Adding test " + oneTest + " to container " + (containerNumber - 1));
          mvn.addTest(oneTest);
        }
      }
    }
  }

  private void addSeparatedQfileTests(ContainerClient containerClient, String label, DTestLogger logger,
                                      String baseDir, Properties masterProperties, int testsPerContainer,
                                      List<ContainerCommand> cmds) throws IOException {
    // Read testconfiguration.properties
    String testPropertiesString = runContainer(containerClient, label, "read-testconfiguration",
        "cat " + masterProperties.getProperty("qFileTests.propertyFiles.mainProperties"), logger);
    Properties testProperties = new Properties();
    testProperties.load(new StringReader(testPropertiesString));

    // Determine the itests to run
    int containerNumber = 1;
    String[] qFileTests = masterProperties.getProperty("qFileTests").trim().split(" ");
    for (String qFileTest : qFileTests) {
      if (qFileTest.toLowerCase().contains("spark")) continue;

      List<String> qFilesToRun;
      switch (qFileTest) {
      case "clientPositive":
        qFilesToRun = findRunnableQFiles(containerClient, label, "find-positive-qfiles", logger,
            testProperties, "ql/src/test/queries/clientpositive");
        break;
      case "miniMr":
        qFilesToRun = new ArrayList<>();
        Collections.addAll(qFilesToRun,
            testProperties.getProperty("minimr.query.files").trim().split(","));
        break;
      case "clientNegative":
        qFilesToRun = findRunnableQFiles(containerClient, label, "find-negative-qfiles", logger,
            testProperties, "ql/src/test/queries/clientnegative");
        break;
      case "miniMrNegative":
        qFilesToRun = new ArrayList<>();
        Collections.addAll(qFilesToRun,
            testProperties.getProperty("minimr.query.negative.files").trim().split(","));
        break;
      case "encryptedCli":
        qFilesToRun = new ArrayList<>();
        Collections.addAll(qFilesToRun,
            testProperties.getProperty("encrypted.query.files").trim().split(","));
        break;
      case "hbasePositive":
        qFilesToRun = findRunnableQFiles(containerClient, label, "find-hbase-qfiles",
            logger, testProperties, "hbase-handler/src/test/queries/positive");
        break;
      case "miniTez":
        qFilesToRun = new ArrayList<>();
        Collections.addAll(qFilesToRun,
            testProperties.getProperty("minitez.query.files").trim().split(","));
        Collections.addAll(qFilesToRun,
            testProperties.getProperty("minitez.query.files.shared").trim().split(","));
        break;
      case "miniLlap":
        qFilesToRun = new ArrayList<>();
        Collections.addAll(qFilesToRun,
            testProperties.getProperty("minillap.query.files").trim().split(","));
        Collections.addAll(qFilesToRun,
            testProperties.getProperty("minillap.shared.query.files").trim().split(","));
        break;
      case "miniLlapLocal":
        qFilesToRun = new ArrayList<>();
        Collections.addAll(qFilesToRun,
            testProperties.getProperty("minillaplocal.query.files").trim().split(","));
        Collections.addAll(qFilesToRun,
            testProperties.getProperty("minillaplocal.shared.query.files").trim().split(","));
        break;
      default:
        throw new RuntimeException("Oops, forgot " + qFileTest);
      }

      Deque<String> qFiles = new ArrayDeque<>(qFilesToRun);
      while (qFiles.size() > 0) {
        MvnCommand mvn = new MvnCommand(baseDir, containerNumber++);
        mvn.addTest(masterProperties.getProperty("qFileTest." + qFileTest + ".driver"));
        for (int i = 0; i < testsPerContainer && qFiles.size() > 0; i++) {
          String oneTest = qFiles.pop();
          LOG.debug("Adding qfile " + oneTest + " to container " + (containerNumber - 1));
          mvn.addQfile(oneTest);
        }
        cmds.add(mvn);
      }
    }


  }

  private List<String> findRunnableQFiles(
      ContainerClient containerClient, String label, String containerName, DTestLogger logger,
      Properties testProperties, String qfileDir) throws IOException {
    // Find all of the positive qfile tests
    String allPositiveQfiles = runContainer(containerClient, label, containerName,
        "find " + qfileDir + " -name \\*.q", logger);

    Set<String> excludedQfiles = new HashSet<>();
    String excludedQfilesStr = testProperties.getProperty("disabled.query.files");
    if (excludedQfilesStr != null && excludedQfilesStr.length() > 0) {
      String[] excludedQfilessArray = excludedQfilesStr.trim().split(",");
      Collections.addAll(excludedQfiles, excludedQfilessArray);
    }

    List<String> runnableQfiles = new ArrayList<>();
    for (String line : allPositiveQfiles.split("\n")) {
      String testPath = line.trim();
      String[] pathElements = testPath.split(File.separator);
      String testName = pathElements[pathElements.length - 1];
      if (!excludedQfiles.contains(testName)) runnableQfiles.add(testName);
    }
    return runnableQfiles;
  }
}
