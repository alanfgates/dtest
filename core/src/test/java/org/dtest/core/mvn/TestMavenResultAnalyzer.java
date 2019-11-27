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
package org.dtest.core.mvn;

import org.dtest.core.BuildState;
import org.dtest.core.Config;
import org.dtest.core.ContainerClient;
import org.dtest.core.ContainerResult;
import org.dtest.core.Reporter;
import org.dtest.core.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestMavenResultAnalyzer {

  @Test
  public void unitTestLog() throws IOException {
    File buildDir = TestUtils.createBuildDir();
    String containerName = "maven-result-analyzer-unit-testlog";
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Config cfg = TestUtils.buildCfg();
    MavenResultAnalyzer analyzer = new MavenResultAnalyzer();
    Reporter reporter = new TestUtils.MockReporter(buildDir);
    ContainerClient client = new TestUtils.MockContainerClient(containerName, "with-error-and-failure", buildDir, 0);
    client.setLog(log);
    client.setConfig(cfg);
    ContainerResult cr = client.runContainer(new TestUtils.MockContainerCommand(containerName, buildDir.getAbsolutePath(), "/bin/bash"));
    client.fetchTestReports(cr, analyzer, reporter, new String[] {"additional.log"});
    analyzer.analyzeLog(cr);

    Assert.assertEquals(1, analyzer.getErrors().size());
    Assert.assertEquals("TestFakeTwo.errorTwo", analyzer.getErrors().get(0));
    Assert.assertEquals(1, analyzer.getFailed().size());
    Assert.assertEquals("TestFake.fail", analyzer.getFailed().get(0));
    Assert.assertEquals(17, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.HAD_FAILURES_OR_ERRORS, analyzer.getBuildState().getState());
    Assert.assertEquals(2, cr.getReports().getKeptFiles().size());
    Assert.assertTrue(cr.getReports().getKeptFiles().containsKey("TestFakeTwo"));
    Assert.assertTrue(cr.getReports().getKeptFiles().containsKey("TestFake"));
    Assert.assertEquals(2, cr.getReports().getKeptFiles().get("TestFakeTwo").size());
    List<File> files = new ArrayList<>(cr.getReports().getKeptFiles().get("TestFakeTwo"));
    Collections.sort(files);
    Assert.assertEquals("additional.log", files.get(0).getName());
    Assert.assertEquals("org.dtest.core.TestFakeTwo.txt", files.get(1).getName());
    Assert.assertEquals(2, cr.getReports().getKeptFiles().get("TestFake").size());
    files = new ArrayList<>(cr.getReports().getKeptFiles().get("TestFake"));
    Collections.sort(files);
    Assert.assertEquals("additional.log", files.get(0).getName());
    Assert.assertEquals("org.dtest.core.TestFake.txt", files.get(1).getName());
    log.dumpToLog();
  }

  @Test
  public void successfulLog() throws IOException {
    File buildDir = TestUtils.createBuildDir();
    String containerName = "maven-result-analyzer-unit-testlog-good";
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Config cfg = TestUtils.buildCfg();
    MavenResultAnalyzer analyzer = new MavenResultAnalyzer();
    Reporter reporter = new TestUtils.MockReporter(buildDir);
    ContainerClient client = new TestUtils.MockContainerClient(containerName, "allgood", buildDir, 0);
    client.setLog(log);
    client.setConfig(cfg);
    ContainerResult cr = client.runContainer(new TestUtils.MockContainerCommand(containerName, buildDir.getAbsolutePath(), "/bin/bash"));
    client.fetchTestReports(cr, analyzer, reporter, null);
    analyzer.analyzeLog(cr);

    Assert.assertEquals(0, analyzer.getErrors().size());
    Assert.assertEquals(0, analyzer.getFailed().size());
    Assert.assertEquals(17, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.SUCCEEDED, analyzer.getBuildState().getState());
    Assert.assertEquals(0, cr.getReports().getKeptFiles().size());
    log.dumpToLog();
  }

  @Test
  public void timeoutLog() throws IOException {
    File buildDir = TestUtils.createBuildDir();
    String containerName = "maven-result-analyzer-unit-testlog-timeout";
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Config cfg = TestUtils.buildCfg();
    MavenResultAnalyzer analyzer = new MavenResultAnalyzer();
    Reporter reporter = new TestUtils.MockReporter(buildDir);
    ContainerClient client = new TestUtils.MockContainerClient(containerName, "timeout", buildDir, 0);
    client.setLog(log);
    client.setConfig(cfg);
    ContainerResult cr = client.runContainer(new TestUtils.MockContainerCommand(containerName, buildDir.getAbsolutePath(), "/bin/bash"));
    client.fetchTestReports(cr, analyzer, reporter, null);
    analyzer.analyzeLog(cr);

    Assert.assertEquals(0, analyzer.getErrors().size());
    Assert.assertEquals(0, analyzer.getFailed().size());
    Assert.assertEquals(18, analyzer.getSucceeded());
    Assert.assertEquals(BuildState.State.HAD_TIMEOUTS, analyzer.getBuildState().getState());
    Assert.assertEquals(0, cr.getReports().getKeptFiles().size());
    log.dumpToLog();
  }

}
