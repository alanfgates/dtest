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

import org.apache.hive.testutils.dtest.BuildInfo;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class MvnCommandFactory extends ContainerCommandFactory {
  private static final Logger LOG = LoggerFactory.getLogger(MvnCommandFactory.class);

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
      MvnCommand mvn = new MvnCommand(containerClient.getContainerBaseDir() + "/" + dir, containerNumber++);
      mvn.setEnv("USER", DockerClient.USER);
      // TODO need to exclude any excluded tests
      cmds.add(mvn);

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
        mvn.setEnv("USER", DockerClient.USER);
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
      cmds.add(mvn);
    }
  }

  // For qfile tests that need split
  private class SplittingSingleTestDirInfo extends SingleTestDirInfo {
    final private Set<String> qFiles;
    final private List<String> isolated;

    SplittingSingleTestDirInfo(String dir, String test, Set<String> qFiles) {
      this(dir, test, qFiles, Collections.emptyList());
    }

    SplittingSingleTestDirInfo(String dir, String test, Set<String> qFiles, List<String> isolated) {
      super(dir, test);
      this.qFiles = qFiles;
      this.isolated = isolated;
    }

    @Override
    void addMvnCommands(ContainerClient containerClient, String label,
                        DTestLogger logger, int testsPerContainer, List<ContainerCommand> cmds)
        throws IOException {
      // Deal with any tests that need to be run alone
      for (String test : isolated) {
        cmds.add(buildOneCommand(containerClient, Collections.singleton(test)));
        qFiles.remove(test);
      }

      while (!qFiles.isEmpty()) {
        List<String> oneSet = new ArrayList<>(testsPerContainer);
        for (String qFile : qFiles) {
          if (oneSet.size() > testsPerContainer) break;
          oneSet.add(qFile);
        }
        cmds.add(buildOneCommand(containerClient, oneSet));
        qFiles.removeAll(oneSet);
      }
    }

    private MvnCommand buildOneCommand(ContainerClient containerClient, Collection<String> qfiles) {
      MvnCommand mvn = new MvnCommand(containerClient.getContainerBaseDir() + "/" + dir, containerNumber++);
      mvn.setEnv("USER", DockerClient.USER);
      mvn.addTest(test);
      for (String qfile : qfiles) {
        LOG.debug("Adding qfile " + qfile + " to container " + (containerNumber - 1));
        mvn.addQfile(qfile);
      }
      return mvn;
    }
  }

  public MvnCommandFactory() {
    excludedTests = new HashSet<>(Arrays.asList("TestHiveMetaStore", "TestSerDe",
        "TestJdbcWithLocalClusterSpark.java", "TestMultiSessionsHS2WithLocalClusterSpark.java",
        "TestSparkStatistics.java", "TestReplicationScenariosAcrossInstances.java",
        "TestReplicationScenariosAcidTables.java"));
    excludedQfiles = new HashSet<>(Arrays.asList("masking_5.q", "orc_merge10.q"));
  }

  @Override
  public List<ContainerCommand> getContainerCommands(ContainerClient containerClient,
                                                     BuildInfo buildInfo,
                                                     DTestLogger logger) throws IOException {

    // Read the test properties file as a number of things need info in there
    String testPropertiesString = runContainer(containerClient, ".", buildInfo.getLabel(), "read-testconfiguration",
        "cat itests/src/test/resources/testconfiguration.properties", logger);
    testProperties = new Properties();
    testProperties.load(new StringReader(testPropertiesString));

    // Figure out which qfiles are excluded
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
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestBeeLineDriver",
        findQFilesFromProperties("beeline.positive.include")));
    testInfos.add(new SingleTestDirInfo("itests/qtest", "TestContribCliDriver"));
    testInfos.add(new SingleTestDirInfo("itests/qtest", "TestContribNegativeCliDriver"));
    testInfos.add(new SingleTestDirInfo("itests/qtest", "TestHBaseNegativeCliDriver"));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestMiniDruidCliDriver",
        findQFilesFromProperties("druid.query.files")));
    //testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestMiniDruidKafkaCliDriver", findQFilesFromProperties("druid.kafka.query.files")));
    testInfos.add(new SingleTestDirInfo("itests/qtest", "TestTezPerfCliDriver"));
    testInfos.add(new SingleTestDirInfo("itests/qtest", "TestParseNegativeDriver"));
    testInfos.add(new SingleTestDirInfo("itests/qtest-accumulo", "TestAccumuloCliDriver"));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestCliDriver",
        findQFilesInDir(containerClient, buildInfo.getLabel(), logger, "ql/src/test/queries/clientpositive"),
        Arrays.asList("authorization_show_grant.q")));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestEncryptedHDFSCliDriver",
        findQFilesFromProperties("encrypted.query.files")));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestNegativeCliDriver",
        findQFilesInDir(containerClient, buildInfo.getLabel(), logger, "ql/src/test/queries/clientnegative")));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestHBaseCliDriver",
        findQFilesInDir(containerClient, buildInfo.getLabel(), logger, "hbase-handler/src/test/queries/positive")));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestMiniTezCliDriver",
        findQFilesFromProperties("minitez.query.files", "minitez.query.files.shared")));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestMiniLlapCliDriver",
        findQFilesFromProperties("minillap.query.files", "minillap.shared.query.files")));
    testInfos.add(new SplittingSingleTestDirInfo("itests/qtest", "TestMiniLlapLocalCliDriver",
        findQFilesFromProperties("minillaplocal.query.files", "minillaplocal.shared.query.files")));

    List<ContainerCommand> cmds = new ArrayList<>();
    int testsPerContainer = Config.TESTS_PER_CONTAINER.getAsInt();
    for (TestDirInfo tdi : testInfos) {
      tdi.addMvnCommands(containerClient, buildInfo.getLabel(), logger, testsPerContainer, cmds);
    }
    return cmds;
  }

  private String runContainer(ContainerClient containerClient, final String dir,
                              final String label,
                              final String containerName,
                              final String cmd, DTestLogger logger) throws IOException {
    ContainerResult result = containerClient.runContainer(300,
        new ContainerCommand() {
          @Override
          public String containerSuffix() {
            return Utils.buildContainerName(label, containerName);
          }

          @Override
          public String[] shellCommand() {
            return Utils.shellCmdInRoot(
                containerClient.getContainerBaseDir() + (dir == null ? "" : "/" + dir), ()-> cmd);
          }

          @Override
          public String containerDirectory() {
            return dir;
          }
        }, logger);
    if (result.getRc() != 0) {
      String msg = "Failed to run cmd " + cmd + " as part of determining tests to run";
      LOG.error(msg);
      throw new IOException(msg);
    }
    return result.getLogs();
  }

  private Set<String> findQFilesFromProperties(String... properties) {
    Set<String> qfiles = new HashSet<>();
    for (String property : properties) {
      Collections.addAll(qfiles, testProperties.getProperty(property).trim().split(","));
    }
    return qfiles;
  }

  private Set<String> findQFilesInDir(ContainerClient containerClient, String label,
                                      DTestLogger logger, String qfileDir) throws IOException {
    // Find all of the qfile tests
    String allPositiveQfiles = runContainer(containerClient, qfileDir, label,
        "qfile-finder-" + containerNumber++, "find . -name \\*.q -maxdepth 1", logger);


    Set<String> runnableQfiles = new HashSet<>();
    for (String line : allPositiveQfiles.split("\n")) {
      String testPath = line.trim();
      String[] pathElements = testPath.split("/");
      String testName = pathElements[pathElements.length - 1];
      if (!excludedQfiles.contains(testName)) runnableQfiles.add(testName);
    }
    return runnableQfiles;
  }
}
