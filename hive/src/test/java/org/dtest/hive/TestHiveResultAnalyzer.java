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
import org.dtest.core.Config;
import org.dtest.core.ContainerClient;
import org.dtest.core.ContainerCommand;
import org.dtest.core.ContainerResult;
import org.dtest.core.Reporter;
import org.dtest.core.ResultAnalyzer;
import org.dtest.core.testutils.TestUtils;
import org.dtest.core.testutils.MockContainerClient;
import org.dtest.core.testutils.MockContainerCommand;
import org.dtest.core.testutils.MockReporter;
import org.dtest.core.testutils.TestLogger;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestHiveResultAnalyzer {

  @Test
  public void unitTestAllGood() throws IOException {
    File buildDir = TestUtils.createBuildDir();
    String containerName = "hive-result-analyzer-unit-testlog-good";
    TestLogger log = new TestLogger();
    ResultAnalyzer analyzer = new HiveResultAnalyzer();
    Reporter reporter = new MockReporter(buildDir);
    ContainerClient client = new MockContainerClient(containerName, "allgood", buildDir, 0);
    client.setLog(log);
    ContainerCommand cmd = new MockContainerCommand(containerName, buildDir.getAbsolutePath(), "/bin/bash");
    ContainerResult cr = client.runContainer(cmd);
    client.fetchTestReports(cr, analyzer, reporter, new String[] {"hive.log"});
    analyzer.analyzeResult(cr, cmd);

    Assert.assertEquals(0, analyzer.getErrors().size());
    Assert.assertEquals(0, analyzer.getFailed().size());
    Assert.assertEquals(216, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.SUCCEEDED, analyzer.getBuildState().getState());
    Assert.assertEquals(0, cr.getReports().getKeptFiles().size());
    log.dumpToLog();

  }

  @Test
  public void unitTestErrorsAndFailures() throws IOException {
    File buildDir = TestUtils.createBuildDir();
    String containerName = "hive-result-analyzer-unit-testlog-errors-and-failures";
    TestLogger log = new TestLogger();
    ResultAnalyzer analyzer = new HiveResultAnalyzer();
    Reporter reporter = new MockReporter(buildDir);
    ContainerClient client = new MockContainerClient(containerName, "with-error-and-failure", buildDir, 0);
    client.setLog(log);
    ContainerCommand cmd = new MockContainerCommand(containerName, buildDir.getAbsolutePath(), "/bin/bash");
    ContainerResult cr = client.runContainer(cmd);
    client.fetchTestReports(cr, analyzer, reporter, new String[] {"hive.log"}); // there isn't actually a hive.log there, make sure that doesn't cause an issue
    analyzer.analyzeResult(cr, cmd);

    Assert.assertEquals(1, analyzer.getErrors().size());
    Assert.assertEquals("TestHiveConfUtil.testError", analyzer.getErrors().get(0));
    Assert.assertEquals(1, analyzer.getFailed().size());
    Assert.assertEquals("TestHiveConf.failingTest", analyzer.getFailed().get(0));
    Assert.assertEquals(216, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.HAD_FAILURES_OR_ERRORS, analyzer.getBuildState().getState());
    Assert.assertEquals(2, cr.getReports().getKeptFiles().size());
    Assert.assertTrue(cr.getReports().getKeptFiles().containsKey("TestHiveConfUtil"));
    Assert.assertTrue(cr.getReports().getKeptFiles().containsKey("TestHiveConf"));
    Assert.assertEquals(2, cr.getReports().getKeptFiles().get("TestHiveConfUtil").size());
    List<File> files = new ArrayList<>(cr.getReports().getKeptFiles().get("TestHiveConfUtil"));
    Collections.sort(files);
    Assert.assertEquals("org.apache.hadoop.hive.conf.TestHiveConfUtil-output.txt", files.get(0).getName());
    Assert.assertEquals("org.apache.hadoop.hive.conf.TestHiveConfUtil.txt", files.get(1).getName());
    Assert.assertEquals(2, cr.getReports().getKeptFiles().get("TestHiveConf").size());
    files = new ArrayList<>(cr.getReports().getKeptFiles().get("TestHiveConf"));
    Collections.sort(files);
    Assert.assertEquals("org.apache.hadoop.hive.conf.TestHiveConf-output.txt", files.get(0).getName());
    Assert.assertEquals("org.apache.hadoop.hive.conf.TestHiveConf.txt", files.get(1).getName());
    log.dumpToLog();
  }

  @Test
  public void timeoutLog() throws IOException {
    File buildDir = TestUtils.createBuildDir();
    String containerName = "maven-result-analyzer-hive-timeout";
    TestLogger log = new TestLogger();
    Config cfg = TestUtils.buildCfg();
    ResultAnalyzer analyzer = new HiveResultAnalyzer();
    Reporter reporter = new MockReporter(buildDir);
    ContainerClient client = new MockContainerClient(containerName, "timeout", buildDir, 0);
    client.setLog(log);
    client.setConfig(cfg);
    ContainerCommand cmd = new MockContainerCommand(containerName, buildDir.getAbsolutePath(), "/bin/bash");
    ContainerResult cr = client.runContainer(cmd);
    client.fetchTestReports(cr, analyzer, reporter, new String[] {"hive.log"});
    analyzer.analyzeResult(cr, cmd);

    Assert.assertEquals(0, analyzer.getErrors().size());
    Assert.assertEquals(0, analyzer.getFailed().size());
    Assert.assertEquals(216, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.HAD_TIMEOUTS, analyzer.getBuildState().getState());
    Assert.assertEquals(0, cr.getReports().getKeptFiles().size());
    log.dumpToLog();
  }

  @Test
  public void qfileAllGood() throws IOException {
    File buildDir = TestUtils.createBuildDir();
    String containerName = "hive-result-analyzer-qfile-good";
    TestLogger log = new TestLogger();
    ResultAnalyzer analyzer = new HiveResultAnalyzer();
    Reporter reporter = new MockReporter(buildDir);
    ContainerClient client = new MockContainerClient(containerName, "qfile-allgood", buildDir, 0);
    client.setLog(log);
    ContainerCommand cmd = new MockContainerCommand(containerName, buildDir.getAbsolutePath(), "/bin/bash");
    ContainerResult cr = client.runContainer(cmd);
    client.fetchTestReports(cr, analyzer, reporter, new String[] {"hive.log"});
    analyzer.analyzeResult(cr, cmd);

    Assert.assertEquals(0, analyzer.getErrors().size());
    Assert.assertEquals(0, analyzer.getFailed().size());
    Assert.assertEquals(3, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.SUCCEEDED, analyzer.getBuildState().getState());
    Assert.assertEquals(0, cr.getReports().getKeptFiles().size());
    log.dumpToLog();
  }

  @Test
  public void qfileFailures() throws IOException {
    File buildDir = TestUtils.createBuildDir();
    String containerName = "hive-result-analyzer-qfile-errors-and-failures";
    TestLogger log = new TestLogger();
    ResultAnalyzer analyzer = new HiveResultAnalyzer();
    Reporter reporter = new MockReporter(buildDir);
    ContainerClient client = new MockContainerClient(containerName, "qfile-with-failure", buildDir, 0);
    client.setLog(log);
    ContainerCommand cmd = new MockContainerCommand(containerName, buildDir.getAbsolutePath(), "/bin/bash");
    ContainerResult cr = client.runContainer(cmd);
    client.fetchTestReports(cr, analyzer, reporter, new String[] {"hive.log"}); // there isn't actually a hive.log there, make sure that doesn't cause an issue
    analyzer.analyzeResult(cr, cmd);

    Assert.assertEquals(0, analyzer.getErrors().size());
    Assert.assertEquals(1, analyzer.getFailed().size());
    Assert.assertEquals("TestCliDriver.selectDistinctStar", analyzer.getFailed().get(0));
    Assert.assertEquals(3, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.HAD_FAILURES_OR_ERRORS, analyzer.getBuildState().getState());
    Assert.assertEquals(1, cr.getReports().getKeptFiles().size());
    Assert.assertTrue(cr.getReports().getKeptFiles().containsKey("TestCliDriver.selectDistinctStar"));
    Assert.assertEquals(3, cr.getReports().getKeptFiles().get("TestCliDriver.selectDistinctStar").size());
    List<File> files = new ArrayList<>(cr.getReports().getKeptFiles().get("TestCliDriver.selectDistinctStar"));
    Collections.sort(files);
    Assert.assertEquals("hive.log", files.get(0).getName());
    Assert.assertEquals("org.apache.hadoop.hive.cli.TestCliDriver-output.txt", files.get(1).getName());
    Assert.assertEquals("org.apache.hadoop.hive.cli.TestCliDriver.txt", files.get(2).getName());
    log.dumpToLog();
  }
}
