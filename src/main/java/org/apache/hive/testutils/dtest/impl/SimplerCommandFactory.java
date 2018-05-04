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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SimplerCommandFactory extends ContainerCommandFactory {
  private static final Logger LOG = LoggerFactory.getLogger(SimplerCommandFactory.class);

  private final Set<String> excludedTests;
  private int containerNumber;
  private Properties testProperties;
  private Set<String> excludedQfiles;

  private abstract class TestDirInfo {
    final String dir;

    TestDirInfo(String dir) {
      this.dir = dir;
    }

    abstract void addMvnCommands(ContainerClient containerClient, String label,
                                 DTestLogger logger, int testsPerContainer, List<ContainerCommand> cmds)
        throws IOException;

  }

  // Adds tests for unit tests that run the whole directory
  private class SimpleDirInfo extends TestDirInfo {
    SimpleDirInfo(String dir) {
      super(dir);
    }

    @Override
    void addMvnCommands(ContainerClient containerClient, String label,
                        DTestLogger logger, int testsPerContainer, List<ContainerCommand> cmds) {
      cmds.add(new MvnCommand(containerClient.getContainerBaseDir() + "/" + dir, containerNumber++));

    }
  }

  // Unit tests for directories that are too big and need to be split
  private class SplittingDirInfo extends TestDirInfo {
    SplittingDirInfo(String dir) {
      super(dir);
    }

    @Override
    void addMvnCommands(ContainerClient containerClient, String label,
                        DTestLogger logger, int testsPerContainer, List<ContainerCommand> cmds) throws
        IOException {
      String unitTests = runContainer(containerClient, dir, label,
          "find-tests-" + containerNumber++, "find . -name Test\\*\\*.java", logger);
      Deque<String> tests = new ArrayDeque<>();
      for (String line : unitTests.split("\n")) {
        String testPath = line.trim();

        // Isolate the test name
        String[] pathElements = testPath.split("/");
        String testName = pathElements[pathElements.length - 1];
        if (!excludedTests.contains(testName)) tests.add(testName);
      }

      while (!tests.isEmpty()) {
        MvnCommand mvn = new MvnCommand(containerClient.getContainerBaseDir() + "/" + dir, containerNumber++);
        for (int i = 0; i < testsPerContainer && !tests.isEmpty(); i++) {
          String single = tests.pop();
          LOG.debug("Adding test " + single + " to container " + (containerNumber - 1));
          mvn.addTest(single);
        }
        cmds.add(mvn);
      }
    }
  }

  // For tests that need to be run alone
  private class SingleTestDirInfo extends TestDirInfo {
    final protected String test;

    SingleTestDirInfo(String dir, String test) {
      super(dir);
      this.test = test;
    }

    @Override
    void addMvnCommands(ContainerClient containerClient, String label,
                        DTestLogger logger, int testsPerContainer, List<ContainerCommand> cmds)
        throws IOException {
      MvnCommand mvn = new MvnCommand(containerClient.getContainerBaseDir() + "/" + dir, containerNumber++);
      mvn.addTest(test);
      mvn.setEnv("USER", DockerClient.USER);
    }
  }

  // For qfile tests that need split
  private class SplittingSingleTestDirInfo extends SingleTestDirInfo {
    final private Deque<String> qFiles;

    SplittingSingleTestDirInfo(String dir, String test,
                                      Deque<String> qFiles) {
      super(dir, test);
      this.qFiles = qFiles;
    }

    @Override
    void addMvnCommands(ContainerClient containerClient, String label,
                        DTestLogger logger, int testsPerContainer, List<ContainerCommand> cmds) throws
        IOException {
      while (!qFiles.isEmpty()) {
        MvnCommand mvn = new MvnCommand(containerClient.getContainerBaseDir() + "/" + dir, containerNumber++);
        mvn.setEnv("USER", DockerClient.USER);
        mvn.addTest(test);
        for (int i = 0; i < testsPerContainer && !qFiles.isEmpty(); i++) {
          String single = qFiles.pop();
          LOG.debug("Adding qfile " + single + " to container " + (containerNumber - 1));
          mvn.addQfile(single);
        }
      }
    }
  }

  public SimplerCommandFactory() {
    excludedTests = new HashSet<>(Arrays.asList("TestHiveMetaStore", "TestSerDe"));
  }

  @Override
  public List<ContainerCommand> getContainerCommands(ContainerClient containerClient, String label,
                                                     DTestLogger logger) throws IOException {

    // Read the test properties file as a number of things need info in there
    String testPropertiesString = runContainer(containerClient, ".", label, "read-testconfiguration",
        "cat itests/src/test/resources/testconfiguration.properties", logger);
    testProperties = new Properties();
    testProperties.load(new StringReader(testPropertiesString));

    // Figure out which qfiles are excluded
    excludedQfiles = new HashSet<>();
    String excludedQfilesStr = testProperties.getProperty("disabled.query.files");
    if (excludedQfilesStr != null && excludedQfilesStr.length() > 0) {
      String[] excludedQfilessArray = excludedQfilesStr.trim().split(",");
      Collections.addAll(excludedQfiles, excludedQfilessArray);
    }

    containerNumber = 0;
    List<TestDirInfo> testInfos = new ArrayList<>();
    testInfos.add(new SimpleDirInfo("accumulo-handler"));
    testInfos.add(new SimpleDirInfo("beeline"));
    testInfos.add(new SimpleDirInfo("cli"));
    testInfos.add(new SimpleDirInfo("common"));
    testInfos.add(new SimpleDirInfo("contrib"));
    testInfos.add(new SimpleDirInfo("hplsql"));
    testInfos.add(new SimpleDirInfo("jdbc"));
    testInfos.add(new SimpleDirInfo("jdbc-handler"));
    testInfos.add(new SimpleDirInfo("serde"));
    testInfos.add(new SimpleDirInfo("shims/0.23"));
    testInfos.add(new SimpleDirInfo("shims/common"));
    testInfos.add(new SimpleDirInfo("storage-api"));
    testInfos.add(new SimpleDirInfo("llap-client"));
    testInfos.add(new SimpleDirInfo("llap-common"));
    testInfos.add(new SimpleDirInfo("llap-server"));
    testInfos.add(new SimpleDirInfo("llap-tez"));
    testInfos.add(new SplittingDirInfo("standalone-metastore"));
    testInfos.add(new SimpleDirInfo("druid-handler"));
    testInfos.add(new SimpleDirInfo("service"));
    testInfos.add(new SimpleDirInfo("spark-client"));
    testInfos.add(new SimpleDirInfo("streaming"));
    testInfos.add(new SimpleDirInfo("hbase-handler"));
    testInfos.add(new SimpleDirInfo("hcatalog/core"));
    testInfos.add(new SimpleDirInfo("hcatalog/hcatalog-pig-adapter"));
    testInfos.add(new SimpleDirInfo("hcatalog/server-extensions"));
    testInfos.add(new SimpleDirInfo("hcatalog/streaming"));
    testInfos.add(new SimpleDirInfo("hcatalog/webhcat/java-client"));
    testInfos.add(new SimpleDirInfo("hcatalog/webhcat/svr"));
    testInfos.add(new SplittingDirInfo("ql"));
    testInfos.add(new SimpleDirInfo("itests/hcatalog-unit"));
    testInfos.add(new SimpleDirInfo("itests/hive-blobstore"));
    testInfos.add(new SimpleDirInfo("itests/hive-minikdc"));
    testInfos.add(new SplittingDirInfo("itests/hive-unit"));
    testInfos.add(new SimpleDirInfo("itests/hive-unit-hadoop2"));
    testInfos.add(new SimpleDirInfo("itests/test-serde"));
    testInfos.add(new SimpleDirInfo("itests/util"));
    testInfos.add(new SingleTestDirInfo("itests/hive-blobstore", "TestBlobstoreCliDriver"));
    testInfos.add(new SingleTestDirInfo("itests/hive-blobstore", "TestBlobstoreNegativeCliDriver"));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestBeeLineDriver", findQFilesFromProperties("beeline.positive.include")));
    testInfos.add(new SingleTestDirInfo("itests/qtest", "TestContribCliDriver"));
    testInfos.add(new SingleTestDirInfo("itests/qtest", "TestContribNegativeCliDriver"));
    testInfos.add(new SingleTestDirInfo("itests/qtest", "TestHBaseNegativeCliDriver"));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestMiniDruidCliDriver", findQFilesFromProperties("druid.query.files")));
    //testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestMiniDruidKafkaCliDriver", findQFilesFromProperties("druid.kafka.query.files")));
    testInfos.add(new SingleTestDirInfo("itests/qtest", "TestTezPerfCliDriver"));
    testInfos.add(new SingleTestDirInfo("itests/qtest", "TestParseNegativeDriver"));
    testInfos.add(new SingleTestDirInfo("itests/qtest-accumulo", "TestAccumuloCliDriver"));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestCliDriver", findQFilesInDir(containerClient, label, logger, "ql/src/test/queries/clientpositive")));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestMinimrCliDriver", findQFilesFromProperties("minimr.query.files")));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestEncryptedHDFSCliDriver", findQFilesFromProperties("encrypted.query.files")));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestNegativeMinimrCliDriver", findQFilesFromProperties("minimr.query.negative.files")));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestNegativeCliDriver", findQFilesInDir(containerClient, label, logger, "ql/src/test/queries/clientnegative")));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestHBaseCliDriver", findQFilesInDir(containerClient, label, logger, "hbase-handler/src/test/queries/positive")));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestMiniTezCliDriver", findQFilesFromProperties("minitez.query.files", "minitez.query.files.shared")));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestMiniLlapCliDriver", findQFilesFromProperties("minillap.query.files", "minillap.shared.query.files")));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestMiniLlapLocalCliDriver", findQFilesFromProperties("minillaplocal.query.files", "minillaplocal.shared.query.files")));

    List<ContainerCommand> cmds = new ArrayList<>();
    int testsPerContainer = Integer.valueOf(System.getProperty(Config.TESTS_PER_CONTAINER, "50"));
    for (TestDirInfo tdi : testInfos) {
      tdi.addMvnCommands(containerClient, label, logger, testsPerContainer, cmds);
    }
    return cmds;
  }

  private String runContainer(ContainerClient containerClient, final String dir,
                              final String label,
                              final String containerName,
                              final String cmd, DTestLogger logger) throws IOException {
    ContainerResult result = containerClient.runContainer(5, TimeUnit.MINUTES,
        new ContainerCommand() {
          @Override
          public String containerName() {
            return Utils.buildContainerName(label, containerName);
          }

          @Override
          public String[] shellCommand() {
            String localDir = containerClient.getContainerBaseDir();
            return Utils.shellCmdInRoot(
                containerClient.getContainerBaseDir() + (dir == null ? "" : "/" + dir), ()-> cmd);
          }
        }, logger);
    if (result.rc != 0) {
      String msg = "Failed to run cmd " + cmd + " as part of determining tests to run";
      LOG.error(msg);
      throw new IOException(msg);
    }
    return result.logs;
  }

  private Deque<String> findQFilesFromProperties(String... properties) {
    Deque<String> qfiles = new ArrayDeque<>();
    for (String property : properties) {
      Collections.addAll(qfiles, testProperties.getProperty(property).trim().split(","));
    }
    return qfiles;
  }

  private Deque<String> findQFilesInDir(ContainerClient containerClient, String label,
                                        DTestLogger logger, String qfileDir) throws IOException {
    // Find all of the qfile tests
    String allPositiveQfiles = runContainer(containerClient, qfileDir, label,
        "qfile-finder-" + containerNumber++, "find . -name \\*.q -maxdepth 1", logger);


    Deque<String> runnableQfiles = new ArrayDeque<>();
    for (String line : allPositiveQfiles.split("\n")) {
      String testPath = line.trim();
      String[] pathElements = testPath.split("/");
      String testName = pathElements[pathElements.length - 1];
      if (!excludedQfiles.contains(testName)) runnableQfiles.add(testName);
    }
    return runnableQfiles;
  }
}
