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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

  private static final String[] TEST_DIRS = {
      "accumulo-handler", "beeline", "cli", "common", "hplsql", "jdbc", "jdbc-handler",
      "serde", "shims", "storage-api", "llap-client", "llap-common", "llap-server",
      "standalone-metastore", "druid-handler", "service", "spark-client", "hbase-handler",
      "hcatalog/core", "hcatalog/hcatalog-pig-adapter", "hcatalog/server-extensions",
      "hcatalog/streaming", "hcatalog/webhcat/java-client", "hcatalog/webhcat/svr", "ql",
      "itests/hcatalog-unit", "itests/hive-blobstore", "itests/hive-minikdc", "itests/hive-unit",
      "itests/hive-unit-hadoop2",
  };

  private static Pattern[] TEST_DIR_PATTERNS = new Pattern[TEST_DIRS.length];

  static {
    for (int i = 0; i < TEST_DIRS.length; i++) {
      TEST_DIR_PATTERNS[i] = Pattern.compile(TEST_DIRS[i] + "/src/test");
    }
  }

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

    addIUnitTests(containerClient, label, logger, baseDir, masterProperties, testsPerContainer,
        cmds);


    // TODO This is turbo brittle.  It should be scanning the source for pom files and adding a
    // command for each, and then counting qfiles and dividing them up.

    // Unit tests
    /*
    cmds.add(new MvnCommand(baseDir, "itests/hive-unit"));
    cmds.add(new MvnCommand(baseDir, "accumulo-handler"));
    cmds.add(new MvnCommand(baseDir, "beeline"));
    cmds.add(new MvnCommand(baseDir, "cli"));
    cmds.add(new MvnCommand(baseDir, "common"));
    cmds.add(new MvnCommand(baseDir, "hplsql"));
    cmds.add(new MvnCommand(baseDir, "jdbc"));
    cmds.add(new MvnCommand(baseDir, "jdbc-handler"));
    cmds.add(new MvnCommand(baseDir, "serde"));
    cmds.add(new MvnCommand(baseDir, "shims"));
    cmds.add(new MvnCommand(baseDir, "storage-api"));
    cmds.add(new MvnCommand(baseDir, "llap-client"));
    cmds.add(new MvnCommand(baseDir, "llap-common"));
    cmds.add(new MvnCommand(baseDir, "llap-server"));
    cmds.add(new MvnCommand(baseDir, "standalone-metastore").addProperty("test.groups", ""));
    cmds.add(new MvnCommand(baseDir, "druid-handler"));
    cmds.add(new MvnCommand(baseDir, "service"));
    cmds.add(new MvnCommand(baseDir, "spark-client"));
    cmds.add(new MvnCommand(baseDir, "hbase-handler"));
    cmds.add(new MvnCommand(baseDir, "hcatalog/core"));
    cmds.add(new MvnCommand(baseDir, "hcatalog/hcatalog-pig-adapter"));
    cmds.add(new MvnCommand(baseDir, "hcatalog/server-extensions"));
    cmds.add(new MvnCommand(baseDir, "hcatalog/streaming"));
    cmds.add(new MvnCommand(baseDir, "hcatalog/webhcat/java-client"));
    cmds.add(new MvnCommand(baseDir, "hcatalog/webhcat/svr"));
    cmds.add(new MvnCommand(baseDir, "ql"));

    // itests junit tests
    cmds.add(new MvnCommand(baseDir, "itests/hcatalog-unit").addExclude("TestSequenceFileReadWrite"));
    cmds.add(new MvnCommand(baseDir, "itests/hive-blobstore"));
    cmds.add(new MvnCommand(baseDir, "itests/hive-minikdc"));
    cmds.add(new MvnCommand(baseDir, "itests/hive-unit-hadoop2"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest-accumulo"));

    // qfile tests
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestBeeLineDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCompareCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestContribCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestContribNegativeCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestEncryptedHDFSCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestHBaseCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestHBaseNegativeCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestMiniDruidCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestMiniLlapCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestMiniLlapLocalCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeMinimrCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestParseNegativeDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestMinimrCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestMiniTezCliDriver"));

    // Super big qfile tests broken out
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("v.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("a[a-t].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("au.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("a[v-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("b.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("c[a-n].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("co.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("c[p-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("d[a-l].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("d[m-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("e.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("[fhkn].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("g.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("i.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("j.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("l.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("m.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("[oq].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("pa.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("p[b-e].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("p[f-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("[rw-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("s[a-d].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("s[e-l].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("s[m-s].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("s[t-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("t.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("u[a-d].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("u[e-z].\\*"));

    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("a[a-t].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("a[u-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("[bd].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("c.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("[e-h].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("i.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("[j-o].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("[p-rtv-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("s.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("u.\\*"));
    */

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
    //Map<String, Deque<String>> unitTestsToRun = new HashMap<>();


    Deque<String> unitTestsToRun = new ArrayDeque<>();
    List<String> driverUnitTestsToRun = new ArrayList<>();
    for (String line : allUnitTests.split("\n")) {
      String testPath = line.trim();
      String[] pathElements = testPath.split(File.separator);
      String testName = pathElements[pathElements.length - 1];
      if (excludedTests.contains(testName)) continue;
      // If this is one of the Driver unit tests and not specially handled, then run it in a
      // separate container.
      Matcher m = DRIVER_PATTERN.matcher(testName);
      if (m.matches()) {
        LOG.debug("Adding driver test " + testName + " to container " + (containerNumber));
        cmds.add(new MvnCommand(baseDir, containerNumber++).addTest(testName));
        driverUnitTestsToRun.add(testName);
      } else {
        unitTestsToRun.add(testName);
      }
    }

    for (String driver : driverUnitTestsToRun) {
      MvnCommand mvn = new MvnCommand(baseDir, containerNumber++);
      LOG.debug("Adding test " + driver + " to container " + (containerNumber - 1));
      mvn.addTest(driver);
      cmds.add(mvn);
    }

    while (unitTestsToRun.size() > 0) {
      MvnCommand mvn = new MvnCommand(baseDir, containerNumber++);
      for (int i = 0; i < testsPerContainer && unitTestsToRun.size() > 0; i++) {
        String oneTest = unitTestsToRun.pop();
        LOG.debug("Adding test " + oneTest + " to container " + (containerNumber - 1));
        mvn.addTest(oneTest);
      }
      cmds.add(mvn);
    }
  }

  private void addIUnitTests(ContainerClient containerClient, String label, DTestLogger logger,
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
      MvnCommand mvn = new MvnCommand(baseDir, containerNumber++);
      mvn.addTest(masterProperties.getProperty("qFileTest." + qFileTest + ".driver"));

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
