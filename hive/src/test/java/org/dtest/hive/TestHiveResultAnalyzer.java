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
package org.dtest.hive;

import org.dtest.core.BuildState;
import org.dtest.core.BuildYaml;
import org.dtest.core.Config;
import org.dtest.core.ContainerCommand;
import org.dtest.core.ContainerResult;
import org.dtest.core.DTestLogger;
import org.dtest.core.Slf4jLogger;
import org.dtest.core.TestUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestHiveResultAnalyzer {

  private static final String LOG_FILE_DIR = System.getProperty("java.io.tmpdir") + "/test-classes/logs-to-test-result-analyzer/";
  static final String MASTER_LOG_FILE_DIR = LOG_FILE_DIR + "master/";
  static final String BRANCH_2_LOG_FILE_DIR = LOG_FILE_DIR + "branch-2.3/";

  private static class SimpleContainerCommand extends ContainerCommand {
    private final String name;
    private final String dir;

    public SimpleContainerCommand(String name, String dir) {
      this.name = name;
      this.dir = dir;
    }

    @Override
    public String containerSuffix() {
      return name;
    }

    @Override
    public String[] shellCommand() {
      return new String[0];
    }

    @Override
    public String containerDirectory() {
      return dir;
    }
  }

  @Test
  public void unitTestWithFailures() throws IOException  {
    DTestLogger log = new Slf4jLogger();
    HiveResultAnalyzer analyzer = new HiveResultAnalyzer();
    analyzer.setConfig(new Config()).setLog(log);
    log.debug("Hi there!");

    ContainerResult cr = new ContainerResult(new SimpleContainerCommand("hive-dtest-1_unittests-hive-unit",
        "/Users/gates/git/hive/itests/hive-unit") , 0, TestUtils.readLogFile(MASTER_LOG_FILE_DIR + File.separator + "fail"));
    analyzer.analyzeLog(cr, getYaml());
    //log.dumpToLog();
    Assert.assertEquals(3, analyzer.getErrors().size());
    Collections.sort(analyzer.getErrors());
    Assert.assertEquals("TestSchemaToolForMetastore.testValidateLocations", analyzer.getErrors().get(0));
    Assert.assertEquals("TestSchemaToolForMetastore.testValidateNullValues", analyzer.getErrors().get(1));
    Assert.assertEquals("TestSchemaToolForMetastore.testValidateSequences", analyzer.getErrors().get(2));
    Assert.assertEquals(0, analyzer.getFailed().size());
    Assert.assertEquals(64, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.HAD_FAILURES_OR_ERRORS, analyzer.getBuildState().getState());
    Assert.assertEquals(3, cr.getLogFilesToFetch().size());
    SortedSet<String> orderedLogFiles = new TreeSet<>(cr.getLogFilesToFetch());
    Iterator iter = orderedLogFiles.iterator();
    Assert.assertEquals("/Users/gates/git/hive/itests/hive-unit/target/surefire-reports/org.apache.hadoop.hive.metastore.tools.TestSchemaToolForMetastore-output.txt", iter.next());
    Assert.assertEquals("/Users/gates/git/hive/itests/hive-unit/target/surefire-reports/org.apache.hadoop.hive.metastore.tools.TestSchemaToolForMetastore.txt", iter.next());
    Assert.assertEquals("/Users/gates/git/hive/itests/hive-unit/target/tmp/log/hive.log", iter.next());
  }

  @Test
  public void unitTestWithFailuresBranch2() throws IOException  {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    HiveResultAnalyzer analyzer = new HiveResultAnalyzer();
    analyzer.setConfig(new Config()).setLog(log);
    ContainerResult cr = new ContainerResult(new SimpleContainerCommand("hive-dtest-1_unittests-hive-unit",
        "/Users/gates/git/hive/itests/hive-unit") , 0, TestUtils.readLogFile(BRANCH_2_LOG_FILE_DIR + File.separator + "fail"));
    analyzer.analyzeLog(cr, getYaml());
    log.dumpToLog();
    Assert.assertEquals(1, analyzer.getErrors().size());
    Assert.assertEquals("TestTxnCommands2WithSplitUpdateAndVectorization.testNonAcidToAcidConversion02", analyzer.getErrors().get(0));
    Assert.assertEquals(0, analyzer.getFailed().size());
    Assert.assertEquals(104, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.HAD_FAILURES_OR_ERRORS, analyzer.getBuildState().getState());
    Assert.assertEquals(3, cr.getLogFilesToFetch().size());
    SortedSet<String> orderedLogFiles = new TreeSet<>(cr.getLogFilesToFetch());
    Iterator iter = orderedLogFiles.iterator();
    Assert.assertEquals("/Users/gates/git/hive/itests/hive-unit/target/surefire-reports/org.apache.hadoop.hive.ql.TestTxnCommands2WithSplitUpdateAndVectorization-output.txt", iter.next());
    Assert.assertEquals("/Users/gates/git/hive/itests/hive-unit/target/surefire-reports/org.apache.hadoop.hive.ql.TestTxnCommands2WithSplitUpdateAndVectorization.txt", iter.next());
    Assert.assertEquals("/Users/gates/git/hive/itests/hive-unit/target/tmp/log/hive.log", iter.next());
  }

  @Test
  public void unitTestAllSucceeded() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    HiveResultAnalyzer analyzer = new HiveResultAnalyzer();
    analyzer.setConfig(new Config()).setLog(log);
    ContainerResult cr = new ContainerResult(new SimpleContainerCommand("hive-dtest-1_unittests-hive-unit",
        "/Users/gates/git/hive/itests/hive-unit") , 0, TestUtils.readLogFile(MASTER_LOG_FILE_DIR + File.separator + "good"));
    analyzer.analyzeLog(cr, getYaml());
    log.dumpToLog();
    Assert.assertEquals(0, analyzer.getErrors().size());
    Assert.assertEquals(0, analyzer.getFailed().size());
    Assert.assertEquals(154, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.SUCCEEDED, analyzer.getBuildState().getState());
    Assert.assertEquals(0, cr.getLogFilesToFetch().size());
  }

  @Test
  public void unitTestAllSucceededBranch2() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    HiveResultAnalyzer analyzer = new HiveResultAnalyzer();
    analyzer.setConfig(new Config()).setLog(log);
    ContainerResult cr = new ContainerResult(new SimpleContainerCommand("hive-dtest-1_unittests-hive-unit",
        "/Users/gates/git/hive/itests/hive-unit") , 0, TestUtils.readLogFile(BRANCH_2_LOG_FILE_DIR + File.separator + "good"));
    analyzer.analyzeLog(cr, getYaml());
    log.dumpToLog();
    Assert.assertEquals(0, analyzer.getErrors().size());
    Assert.assertEquals(0, analyzer.getFailed().size());
    Assert.assertEquals(133, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.SUCCEEDED, analyzer.getBuildState().getState());
    Assert.assertEquals(0, cr.getLogFilesToFetch().size());
  }

  @Test
  public void qtestLogWithFailures() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    HiveResultAnalyzer analyzer = new HiveResultAnalyzer();
    analyzer.setConfig(new Config()).setLog(log);
    ContainerResult cr = new ContainerResult(new SimpleContainerCommand(
        "hive-dtest-1_itests-qtest_TestNegativeCliDriver_a_LF_a-t_RT_._S_",
        "/Users/gates/git/hive/itests/qtest"), 0, TestUtils.readLogFile(MASTER_LOG_FILE_DIR + File.separator + "qfile-fail"));
    analyzer.analyzeLog(cr, getYaml());
    log.dumpToLog();
    Assert.assertEquals(0, analyzer.getErrors().size());
    Assert.assertEquals(1, analyzer.getFailed().size());
    Assert.assertEquals("TestCliDriver.show_functions", analyzer.getFailed().get(0));
    Assert.assertEquals(9, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.HAD_FAILURES_OR_ERRORS, analyzer.getBuildState().getState());
    Assert.assertEquals(3, cr.getLogFilesToFetch().size());
    SortedSet<String> orderedLogFiles = new TreeSet<>(cr.getLogFilesToFetch());
    Iterator iter = orderedLogFiles.iterator();
    Assert.assertEquals("/Users/gates/git/hive/itests/qtest/target/surefire-reports/org.apache.hadoop.hive.cli.TestCliDriver-output.txt", iter.next());
    Assert.assertEquals("/Users/gates/git/hive/itests/qtest/target/surefire-reports/org.apache.hadoop.hive.cli.TestCliDriver.txt", iter.next());
    Assert.assertEquals("/Users/gates/git/hive/itests/qtest/target/tmp/log/hive.log", iter.next());
  }

  @Test
  public void qtestLogWithFailuresBranch2() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    HiveResultAnalyzer analyzer = new HiveResultAnalyzer();
    analyzer.setConfig(new Config()).setLog(log);
    ContainerResult cr = new ContainerResult(new SimpleContainerCommand(
        "hive-dtest-1_itests-qtest_TestNegativeCliDriver_a_LF_a-t_RT_._S_",
        "/Users/gates/git/hive/itests/qtest"), 0, TestUtils.readLogFile(BRANCH_2_LOG_FILE_DIR + File.separator + "qfile-fail"));
    analyzer.analyzeLog(cr, getYaml());
    log.dumpToLog();
    Assert.assertEquals(0, analyzer.getErrors().size());
    Assert.assertEquals(1, analyzer.getFailed().size());
    Assert.assertEquals("TestMiniLlapCliDriver.insert_values_orig_table.", analyzer.getFailed().get(0));
    Assert.assertEquals(9, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.HAD_FAILURES_OR_ERRORS, analyzer.getBuildState().getState());
    Assert.assertEquals(3, cr.getLogFilesToFetch().size());
    SortedSet<String> orderedLogFiles = new TreeSet<>(cr.getLogFilesToFetch());
    Iterator iter = orderedLogFiles.iterator();
    Assert.assertEquals("/Users/gates/git/hive/itests/qtest/target/surefire-reports/org.apache.hadoop.hive.cli.TestMiniLlapCliDriver-output.txt", iter.next());
    Assert.assertEquals("/Users/gates/git/hive/itests/qtest/target/surefire-reports/org.apache.hadoop.hive.cli.TestMiniLlapCliDriver.txt", iter.next());
    Assert.assertEquals("/Users/gates/git/hive/itests/qtest/target/tmp/log/hive.log", iter.next());
  }

  @Test
  public void qtestLogAllSuccess() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    HiveResultAnalyzer analyzer = new HiveResultAnalyzer();
    analyzer.setConfig(new Config()).setLog(log);
    ContainerResult cr = new ContainerResult(new SimpleContainerCommand(
        "hive-dtest-1_itests-qtest_TestNegativeCliDriver_a_LF_a-t_RT_._S_",
        "/Users/gates/git/hive/itests/qtest"), 0, TestUtils.readLogFile(MASTER_LOG_FILE_DIR + File.separator + "qfile-good"));
    analyzer.analyzeLog(cr, getYaml());
    log.dumpToLog();
    Assert.assertEquals(0, analyzer.getErrors().size());
    Assert.assertEquals(0, analyzer.getFailed().size());
    Assert.assertEquals(10, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.SUCCEEDED, analyzer.getBuildState().getState());
    Assert.assertEquals(0, cr.getLogFilesToFetch().size());
  }

  @Test
  public void qtestLogAllSuccessBranch2() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    HiveResultAnalyzer analyzer = new HiveResultAnalyzer();
    analyzer.setConfig(new Config()).setLog(log);
    ContainerResult cr = new ContainerResult(new SimpleContainerCommand(
        "hive-dtest-1_itests-qtest_TestNegativeCliDriver_a_LF_a-t_RT_._S_",
        "/Users/gates/git/hive/itests/qtest"), 0, TestUtils.readLogFile(BRANCH_2_LOG_FILE_DIR + File.separator + "qfile-good"));
    analyzer.analyzeLog(cr, getYaml());
    log.dumpToLog();
    Assert.assertEquals(0, analyzer.getErrors().size());
    Assert.assertEquals(0, analyzer.getFailed().size());
    Assert.assertEquals(10, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.SUCCEEDED, analyzer.getBuildState().getState());
    Assert.assertEquals(0, cr.getLogFilesToFetch().size());
  }

  @Test
  public void logWithSkip() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    HiveResultAnalyzer analyzer = new HiveResultAnalyzer();
    analyzer.setConfig(new Config()).setLog(log);
    ContainerResult cr = new ContainerResult(new SimpleContainerCommand(
        "unittest-7", "/Users/gates/git/hive/jdbc"), 0, TestUtils.readLogFile(MASTER_LOG_FILE_DIR + File.separator + "skip"));
    analyzer.analyzeLog(cr, getYaml());
    log.dumpToLog();
    Assert.assertEquals(26, analyzer.getSucceeded());
    Assert.assertEquals(0, analyzer.getErrors().size());
    Assert.assertEquals(0, analyzer.getFailed().size());
    Assert.assertEquals(BuildState.State.SUCCEEDED, analyzer.getBuildState().getState());
  }

  @Test
  public void logWithSkipBranch2() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    HiveResultAnalyzer analyzer = new HiveResultAnalyzer();
    analyzer.setConfig(new Config()).setLog(log);
    ContainerResult cr = new ContainerResult(new SimpleContainerCommand(
        "unittest-7", "/Users/gates/git/hive/jdbc"), 0, TestUtils.readLogFile(BRANCH_2_LOG_FILE_DIR + File.separator + "skip"));
    analyzer.analyzeLog(cr, getYaml());
    log.dumpToLog();
    Assert.assertEquals(12, analyzer.getSucceeded());
    Assert.assertEquals(0, analyzer.getErrors().size());
    Assert.assertEquals(0, analyzer.getFailed().size());
    Assert.assertEquals(BuildState.State.SUCCEEDED, analyzer.getBuildState().getState());
  }

  @Test
  public void timeoutLog() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    HiveResultAnalyzer analyzer = new HiveResultAnalyzer();
    analyzer.setConfig(new Config()).setLog(log);
    analyzer.analyzeLog(new ContainerResult(new SimpleContainerCommand("bla", "bla"), 0,
        TestUtils.readLogFile(MASTER_LOG_FILE_DIR + File.separator + "timeout")), getYaml());
    log.dumpToLog();
    Assert.assertEquals(BuildState.State.HAD_TIMEOUTS, analyzer.getBuildState().getState());
  }

  @Test
  @Ignore // Need to find a branch 2 timeout example
  public void timeoutLogBranch2() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    HiveResultAnalyzer analyzer = new HiveResultAnalyzer();
    analyzer.setConfig(new Config()).setLog(log);
    analyzer.analyzeLog(new ContainerResult(new SimpleContainerCommand("bla", "bla"), 0,
        TestUtils.readLogFile(BRANCH_2_LOG_FILE_DIR + File.separator + "timeout")), getYaml());
    log.dumpToLog();
    Assert.assertEquals(BuildState.State.HAD_TIMEOUTS, analyzer.getBuildState().getState());
  }

  private BuildYaml getYaml() {
    HiveBuildYaml yaml = new HiveBuildYaml();
    yaml.setBaseImage("centos");
    yaml.setRequiredPackages(new String[] {"java-1.8.0-openjdk-devel"});
    yaml.setProjectName("dtest");
    yaml.setJavaPackages(new String[] {"org.apache.hadoop.hive", "org.apache.hive"});
    HiveModuleDirectory[] dirs = new HiveModuleDirectory[2];
    dirs[0] = new HiveModuleDirectory();
    dirs[0].setDir("core");
    dirs[1] = new HiveModuleDirectory();
    dirs[1].setDir("maven");
    yaml.setHiveDirs(dirs);
    yaml.setAdditionalLogs(new String[] {"target/tmp/log/hive.log"});
    return yaml;
  }

  /*
  private static final String LOG_UNIT_TESTS_WITH_FAILURES =
      "[INFO] ------------------------------------------------------------------------\n" +
      "[INFO] Building Hive Integration - Unit Tests 3.0.0-SNAPSHOT\n" +
      "[INFO] ------------------------------------------------------------------------\n" +
      "[WARNING] The POM for net.minidev:json-smart:jar:2.3-SNAPSHOT is missing, no dependency information available\n" +
      "[WARNING] The POM for org.glassfish:javax.el:jar:3.0.1-b06-SNAPSHOT is missing, no dependency information available\n" +
      "[WARNING] The POM for org.glassfish:javax.el:jar:3.0.1-b07-SNAPSHOT is missing, no dependency information available\n" +
      "[WARNING] The POM for org.glassfish:javax.el:jar:3.0.1-b08-SNAPSHOT is missing, no dependency information available\n" +
      "[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 27.497 s - in org.apache.hadoop.hive.ql.txn.compactor.TestCleanerWithReplication\n" +
      "[INFO] Running org.apache.hadoop.hive.ql.txn.compactor.TestCompactor\n" +
      "[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 207.35 s - in org.apache.hadoop.hive.ql.txn.compactor.TestCompactor\n" +
      "[ERROR] Tests run: 11, Failures: 0, Errors: 1, Skipped: 1, Time elapsed: 328.082 s <<< FAILURE! - in org.apache.hadoop.hive.ql.TestAcidOnTez\n" +
      "[ERROR] testGetSplitsLocks(org.apache.hadoop.hive.ql.TestAcidOnTez)  Time elapsed: 21.572 s  <<< ERROR!\n" +
      "java.io.IOException: org.apache.hadoop.hive.ql.metadata.HiveException: java.io.IOException: java.lang.NullPointerException\n" +
      "at org.apache.hadoop.hive.ql.exec.FetchTask.fetch(FetchTask.java:161)\n" +
      "at org.apache.hadoop.hive.ql.Driver.getResults(Driver.java:2424)\n" +
      "at org.apache.hadoop.hive.ql.reexec.ReExecDriver.getResults(ReExecDriver.java:215)\n" +
      "at org.apache.hadoop.hive.ql.TestAcidOnTez.runStatementOnDriver(TestAcidOnTez.java:879)\n" +
      "[ERROR] Tests run: 4, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 30.526 s <<< FAILURE! - in org.apache.hive.jdbc.TestActivePassiveHA\n" +
      "[ERROR] testManualFailover(org.apache.hive.jdbc.TestActivePassiveHA)  Time elapsed: 1.665 s  <<< FAILURE!\n" +
      "java.lang.AssertionError: expected:<true> but was:<false>\n" +
      "at org.junit.Assert.fail(Assert.java:88)\n" +
      "at org.junit.Assert.failNotEquals(Assert.java:743)\n" +
      "at org.junit.Assert.assertEquals(Assert.java:118)\n" +
      "at org.junit.Assert.assertEquals(Assert.java:144)\n" +
      "[INFO] Results:\n" +
      "[INFO]\n" +
      "[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0\n";

  private static final String LOG_UNIT_TESTS_ALL_SUCCEEDED =
      "[INFO] ------------------------------------------------------------------------\n" +
          "[INFO] Building Hive Integration - Unit Tests 3.0.0-SNAPSHOT\n" +
          "[INFO] ------------------------------------------------------------------------\n" +
          "[WARNING] The POM for net.minidev:json-smart:jar:2.3-SNAPSHOT is missing, no dependency information available\n" +
          "[WARNING] The POM for org.glassfish:javax.el:jar:3.0.1-b06-SNAPSHOT is missing, no dependency information available\n" +
          "[WARNING] The POM for org.glassfish:javax.el:jar:3.0.1-b07-SNAPSHOT is missing, no dependency information available\n" +
          "[WARNING] The POM for org.glassfish:javax.el:jar:3.0.1-b08-SNAPSHOT is missing, no dependency information available\n" +
          "[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 27.497 s - in org.apache.hadoop.hive.ql.txn.compactor.TestCleanerWithReplication\n" +
          "[INFO] Running org.apache.hadoop.hive.ql.txn.compactor.TestCompactor\n" +
          "[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 207.35 s - in org.apache.hadoop.hive.ql.txn.compactor.TestCompactor\n" +
          "[INFO] Results:\n" +
          "[INFO]\n" +
          "[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0\n";

  @VisibleForTesting
  static final String LOG_QFILE_WITH_FAILURES =
      "main:\n" +
          "[delete] Deleting directory /root/hive/itests/qtest/target/tmp\n" +
          "[delete] Deleting directory /root/hive/itests/qtest/target/testconf\n" +
          "[delete] Deleting directory /root/hive/itests/qtest/target/warehouse\n" +
          "[mkdir] Created dir: /root/hive/itests/qtest/target/tmp\n" +
          "[mkdir] Created dir: /root/hive/itests/qtest/target/warehouse\n" +
          "[mkdir] Created dir: /root/hive/itests/qtest/target/testconf\n" +
          "[copy] Copying 19 files to /root/hive/itests/qtest/target/testconf\n" +
          "[INFO] Executed tasks\n" +
          "[INFO] Running org.apache.hadoop.hive.cli.TestNegativeCliDriver\n" +
          "[ERROR] Tests run: 74, Failures: 1, Errors: 1, Skipped: 0, Time elapsed: 201.001 s <<< FAILURE! - in org.apache.hadoop.hive.cli.TestNegativeCliDriver\n" +
          "[ERROR] testCliDriver[alter_notnull_constraint_violation](org.apache.hadoop.hive.cli.TestNegativeCliDriver)  Time elapsed: 2.699 s  <<< ERROR!\n" +
          "org.apache.hadoop.hive.ql.exec.errors.DataConstraintViolationError: NOT NULL constraint violated!\n" +
          "at org.apache.hadoop.hive.ql.udf.generic.GenericUDFEnforceNotNullConstraint.evaluate(GenericUDFEnforceNotNullConstraint.java:61)\n" +
          "[ERROR] testCliDriver[alter_table_constraint_duplicate_pk](org.apache.hadoop.hive.cli.TestNegativeCliDriver)  Time elapsed: 0.428 s  <<< FAILURE!\n" +
          "java.lang.AssertionError:\n" +
          "Client Execution succeeded but contained differences (error code = 1) after executing alter_table_constraint_duplicate_pk.q\n" +
          "11c11\n" +
          "< FAILED: Execution Error, return code 1 from org.apache.hadoop.hive.ql.exec.DDLTask. MetaException(message: Primary key already exists for: hive.default.table1)\n" +
          "---\n" +
          "> FAILED: Execution Error, return code 1 from org.apache.hadoop.hive.ql.exec.DDLTask. MetaException(message: Primary key already exists for: default.table1)\n" +
          "\n" +
          "at org.junit.Assert.fail(Assert.java:88)\n" +
          "at org.apache.hadoop.hive.ql.QTestUtil.failedDiff(QTestUtil.java:2166)";

  @VisibleForTesting
  private static final String LOG_QFILE_ALL_SUCCEEDED =
      "main:\n" +
          "[delete] Deleting directory /root/hive/itests/qtest/target/tmp\n" +
          "[delete] Deleting directory /root/hive/itests/qtest/target/testconf\n" +
          "[delete] Deleting directory /root/hive/itests/qtest/target/warehouse\n" +
          "[mkdir] Created dir: /root/hive/itests/qtest/target/tmp\n" +
          "[mkdir] Created dir: /root/hive/itests/qtest/target/warehouse\n" +
          "[mkdir] Created dir: /root/hive/itests/qtest/target/testconf\n" +
          "[copy] Copying 19 files to /root/hive/itests/qtest/target/testconf\n" +
          "[INFO] Executed tasks\n" +
          "[INFO] Running org.apache.hadoop.hive.cli.TestNegativeCliDriver\n" +
          "[INFO] Tests run: 74, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 201.001 s - in org.apache.hadoop.hive.cli.TestNegativeCliDriver\n";

  @VisibleForTesting
  private static final String LOG_WITH_TIMEOUT =
      "[INFO] -------------------------------------------------------\n" +
      "[INFO]  T E S T S \n" +
      "[INFO] -------------------------------------------------------\n" +
      "[INFO] Running org.apache.hadoop.hive.metastore.client.TestAddPartitions \n" +
      "[INFO]\n" +
      "[INFO] Results:\n" +
      "[INFO]\n" +
      "[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0\n" +
      "[INFO]\n" +
      "[INFO] ------------------------------------------------------------------------\n" +
      "[INFO] BUILD FAILURE\n" +
      "[INFO] ------------------------------------------------------------------------\n" +
      "[INFO] Total time: 21.911 s\n" +
      "[INFO] Finished at: 2018-04-03T14:12:45-07:00\n" +
      "[INFO] Final Memory: 54M/849M\n" +
      "[INFO] ------------------------------------------------------------------------\n" +
      "[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:2.20.1:test (default-test) on project hive-standalone-metastore: There was a timeout or other error in the fork -> [Help 1]";

  private static final String LOG_WITH_SKIPPED_TESTS =
      "[INFO] -------------------------------------------------------\n" +
      "[INFO]  T E S T S\n" +
      "[INFO] -------------------------------------------------------\n" +
      "[INFO] Running org.apache.hive.storage.jdbc.TestQueryConditionBuilder\n" +
      "[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.819 s - in org.apache.hive.storage.jdbc.TestQueryConditionBuilder\n" +
      "[INFO] Running org.apache.hive.storage.jdbc.dao.TestGenericJdbcDatabaseAccessor\n" +
      "[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.695 s - in org.apache.hive.storage.jdbc.dao.TestGenericJdbcDatabaseAccessor\n" +
      "[INFO] Running org.apache.hive.storage.jdbc.TestJdbcInputFormat\n" +
      "[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.116 s - in org.apache.hive.storage.jdbc.TestJdbcInputFormat\n" +
      "[INFO] Running org.apache.hive.config.TestJdbcStorageConfigManager\n" +
      "[WARNING] Tests run: 4, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 2.468 s - in org.apache.hive.config.TestJdbcStorageConfigManager\n" +
      "[INFO]\n" +
      "[INFO] Results:\n" +
      "[INFO]\n" +
      "[WARNING] Tests run: 26, Failures: 0, Errors: 0, Skipped: 1\n" +
      "[INFO]\n" +
      "[INFO] ------------------------------------------------------------------------\n" +
      "[INFO] BUILD SUCCESS\n" +
      "[INFO] ------------------------------------------------------------------------\n" +
      "[INFO] Total time: 38.484s\n" +
      "[INFO] Finished at: Wed Aug 01 00:37:35 UTC 2018\n" +
      "[INFO] Final Memory: 56M/2370M\n" +
      "[INFO] ------------------------------------------------------------------------";
      */
}
