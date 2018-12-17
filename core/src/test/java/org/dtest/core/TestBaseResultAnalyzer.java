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

import com.google.common.annotations.VisibleForTesting;
import org.dtest.core.BaseResultAnalyzer;
import org.dtest.core.ContainerCommand;
import org.dtest.core.ContainerResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestBaseResultAnalyzer {
  private static class SimpleContainerCommand extends ContainerCommand {
    private final String name;
    private final String dir;

    SimpleContainerCommand(String name, String dir) {
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
  public void unitTestLog() {
    BaseResultAnalyzer analyzer = new BaseResultAnalyzer();
    ContainerResult cr = new ContainerResult(new SimpleContainerCommand("hive-dtest-1_unittests-hive-unit",
        "/Users/gates/git/hive/itests/hive-unit") , 0, LOG1);
    analyzer.analyzeLog(cr);
    Assert.assertEquals(1, analyzer.getErrors().size());
    Assert.assertEquals("TestAcidOnTez.testGetSplitsLocks", analyzer.getErrors().get(0));
    Assert.assertEquals(1, analyzer.getFailed().size());
    Assert.assertEquals("TestActivePassiveHA.testManualFailover", analyzer.getFailed().get(0));
    Assert.assertEquals(32, analyzer.getSucceeded());
    Assert.assertFalse(analyzer.hadTimeouts());
    Assert.assertTrue(analyzer.runSucceeded());
    Assert.assertEquals(5, cr.getLogFilesToFetch().size());
    SortedSet<String> orderedLogFiles = new TreeSet<>(cr.getLogFilesToFetch());
    Iterator iter = orderedLogFiles.iterator();
    Assert.assertEquals("/Users/gates/git/hive/itests/hive-unit/target/surefire-reports/org.dtest.TestAcidOnTez-output.txt", iter.next());
    Assert.assertEquals("/Users/gates/git/hive/itests/hive-unit/target/surefire-reports/org.dtest.TestAcidOnTez.txt", iter.next());
    Assert.assertEquals("/Users/gates/git/hive/itests/hive-unit/target/surefire-reports/org.dtest.TestActivePassiveHA-output.txt", iter.next());
    Assert.assertEquals("/Users/gates/git/hive/itests/hive-unit/target/surefire-reports/org.dtest.TestActivePassiveHA.txt", iter.next());
    Assert.assertEquals("/Users/gates/git/hive/itests/hive-unit/target/tmp/log/hive.log", iter.next());
  }

  @Test
  public void timeoutLog() {
    BaseResultAnalyzer analyzer = new BaseResultAnalyzer();
    analyzer.analyzeLog(new ContainerResult(new SimpleContainerCommand("bla", "bla"), 0, LOG3));
    Assert.assertTrue(analyzer.hadTimeouts());
    Assert.assertTrue(analyzer.runSucceeded());
  }

  @VisibleForTesting
  public static final String LOG1 =
      "[INFO] ------------------------------------------------------------------------\n" +
          "[INFO] Building Hive Integration - Unit Tests 3.0.0-SNAPSHOT\n" +
          "[INFO] ------------------------------------------------------------------------\n" +
          "[WARNING] The POM for net.minidev:json-smart:jar:2.3-SNAPSHOT is missing, no dependency information available\n" +
          "[WARNING] The POM for org.glassfish:javax.el:jar:3.0.1-b06-SNAPSHOT is missing, no dependency information available\n" +
          "[WARNING] The POM for org.glassfish:javax.el:jar:3.0.1-b07-SNAPSHOT is missing, no dependency information available\n" +
          "[WARNING] The POM for org.glassfish:javax.el:jar:3.0.1-b08-SNAPSHOT is missing, no dependency information available\n" +
          "[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 27.497 s - in org.dtest.TestCleanerWithReplication\n" +
          "[INFO] Running org.dtest.TestCompactor\n" +
          "[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 207.35 s - in org.dtest.TestCompactor\n" +
          "[ERROR] Tests run: 11, Failures: 0, Errors: 1, Skipped: 1, Time elapsed: 328.082 s <<< FAILURE! - in org.dtest.TestAcidOnTez\n" +
          "[ERROR] testGetSplitsLocks(org.dtest.TestAcidOnTez)  Time elapsed: 21.572 s  <<< ERROR!\n" +
          "java.io.IOException: org.dtest.HiveException: java.io.IOException: java.lang.NullPointerException\n" +
          "at org.dtest.FetchTask.fetch(FetchTask.java:161)\n" +
          "at org.dtest.Driver.getResults(Driver.java:2424)\n" +
          "at org.dtest.ReExecDriver.getResults(ReExecDriver.java:215)\n" +
          "at org.dtest.TestAcidOnTez.runStatementOnDriver(TestAcidOnTez.java:879)\n" +
          "[ERROR] Tests run: 4, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 30.526 s <<< FAILURE! - in org.dtest.TestActivePassiveHA\n" +
          "[ERROR] testManualFailover(org.dtest.TestActivePassiveHA)  Time elapsed: 1.665 s  <<< FAILURE!\n" +
          "java.lang.AssertionError: expected:<true> but was:<false>\n" +
          "at org.junit.Assert.fail(Assert.java:88)\n" +
          "at org.junit.Assert.failNotEquals(Assert.java:743)\n" +
          "at org.junit.Assert.assertEquals(Assert.java:118)\n" +
          "at org.junit.Assert.assertEquals(Assert.java:144)\n" +
          "[INFO] Results:\n" +
          "[INFO]\n" +
          "[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0\n";

  @VisibleForTesting
  public static final String LOG3 =
      "[INFO] -------------------------------------------------------\n" +
          "[INFO]  T E S T S \n" +
          "[INFO] -------------------------------------------------------\n" +
          "[INFO] Running org.dtest.TestAddPartitions \n" +
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
