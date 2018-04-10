package org.apache.hive.testutils.dtest;

import org.junit.Assert;
import org.junit.Test;

/**
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
public class TestSimpleResultAnalyzer {

  @Test
  public void unitTestLog() {
    SimpleResultAnalyzer analyzer = new SimpleResultAnalyzer();
    analyzer.analyzeLog(new ContainerResult("hive-dtest-1_itests-hive-unit", 0, LOG1));
    Assert.assertEquals(1, analyzer.getErrors().size());
    Assert.assertEquals("TestAcidOnTez.testGetSplitsLocks", analyzer.getErrors().get(0));
    Assert.assertEquals(1, analyzer.getFailed().size());
    Assert.assertEquals("TestActivePassiveHA.testManualFailover", analyzer.getFailed().get(0));
    Assert.assertEquals(32, analyzer.getSucceeded());
    Assert.assertFalse(analyzer.hadTimeouts());
    Assert.assertTrue(analyzer.runSucceeded());
  }

  @Test
  public void qtestLog() {
    SimpleResultAnalyzer analyzer = new SimpleResultAnalyzer();
    analyzer.analyzeLog(new ContainerResult(
        "hive-dtest-1_itests-qtest_TestNegativeCliDriver_a_LF_a-t_RT_._S_", 1, LOG2));
    Assert.assertEquals(1, analyzer.getErrors().size());
    Assert.assertEquals("TestNegativeCliDriver.alter_notnull_constraint_violation", analyzer.getErrors().get(0));
    Assert.assertEquals(1, analyzer.getFailed().size());
    Assert.assertEquals("TestNegativeCliDriver.alter_table_constraint_duplicate_pk", analyzer.getFailed().get(0));
    Assert.assertEquals(72, analyzer.getSucceeded());
    Assert.assertFalse(analyzer.hadTimeouts());
    Assert.assertTrue(analyzer.runSucceeded());
  }

  @Test
  public void timeoutLog() {
    SimpleResultAnalyzer analyzer = new SimpleResultAnalyzer();
    analyzer.analyzeLog(new ContainerResult("bla", 0, LOG3));
    Assert.assertTrue(analyzer.hadTimeouts());
    Assert.assertTrue(analyzer.runSucceeded());
  }

  @Test
  public void failedRun() {
    SimpleResultAnalyzer analyzer = new SimpleResultAnalyzer();
    analyzer.analyzeLog(new ContainerResult(
        "hive-dtest-1_itests-qtest_TestNegativeCliDriver_a_LF_a-t_RT_._S_", 2, LOG2));
    Assert.assertFalse(analyzer.runSucceeded());
  }

  static final String LOG1 =
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

  static final String LOG2 =
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

  static final String LOG3 =
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
}
