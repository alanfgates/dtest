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

import org.dtest.core.mvn.MavenResultAnalyzer;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TestHtmlReporter {

  @Test
  public void allGood() throws IOException {
    File buildDir = TestUtils.createBuildDir();
    Config cfg = TestUtils.buildCfg();
    String containerName = "reporter-good";
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    ResultAnalyzer analyzer = new MavenResultAnalyzer();
    ContainerClient client = new TestUtils.MockContainerClient(containerName, "allgood", buildDir, 0);
    client.setLog(log);
    client.setConfig(cfg);
    client.setBuildInfo(new TestUtils.MockBuildInfo(buildDir));
    ContainerResult cr = client.runContainer(new TestUtils.MockContainerCommand(containerName, buildDir.getAbsolutePath(), "/bin/bash"));
    Reporter reporter = Reporter.getInstance(cfg, log);
    Assert.assertTrue(reporter instanceof HtmlReporter);
    reporter.setRepo("github")
        .setBranch("new-feature")
        .setProfile("master")
        .setBuildInfo(client.buildInfo);
    client.fetchTestReports(cr, analyzer, reporter, null);
    analyzer.analyzeLog(cr);
    reporter.summarize(analyzer);
    reporter.addFailedTests(client, cr);
    reporter.publish();

    TestUtils.assertFile("<html>\n" +
        "<head>\n" +
        "<title>Docker Test</title>\n" +
        "</head>\n" +
        "<body>\n" +
        "<h1>Status:  SUCCEEDED</h1>\n" +
        "<p>Repository:  github</p>\n" +
        "<p>Branch:  new-feature</p>\n" +
        "<p>Profile:  master</p>\n" +
        "<p><b>Counts:  Succeeded:  17, Errors:  0, Failures:  0</b></p>\n" +
        "<p>Logfile from build: <a href=\"dtest.log\">dtest.log</a></p>\n" +
        "<p>Dockerfile used for build: <a href=\"Dockerfile\">Dockerfile</a></p>\n" +
        "</body>\n" +
        "</html>\n", new File(client.getContainerBaseDir(), "index.html"));
  }

  @Test
  public void failedTest() throws IOException {
    File buildDir = TestUtils.createBuildDir();
    Config cfg = TestUtils.buildCfg();
    String containerName = "reporter-fail";
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    ResultAnalyzer analyzer = new MavenResultAnalyzer();
    ContainerClient client = new TestUtils.MockContainerClient(containerName, "with-error-and-failure", buildDir, 0);
    client.setLog(log);
    client.setConfig(cfg);
    client.setBuildInfo(new TestUtils.MockBuildInfo(buildDir));
    ContainerResult cr = client.runContainer(new TestUtils.MockContainerCommand(containerName, buildDir.getAbsolutePath(), "/bin/bash"));
    Reporter reporter = Reporter.getInstance(cfg, log);
    Assert.assertTrue(reporter instanceof HtmlReporter);
    reporter.setRepo("github")
        .setBranch("new-feature")
        .setProfile("master")
        .setBuildInfo(client.buildInfo);
    client.fetchTestReports(cr, analyzer, reporter, null);
    analyzer.analyzeLog(cr);
    reporter.summarize(analyzer);
    reporter.addFailedTests(client, cr);
    reporter.publish();

    TestUtils.assertFile("<html>\n" +
        "<head>\n" +
        "<title>Docker Test</title>\n" +
        "</head>\n" +
        "<body>\n" +
        "<h1>Status:  HAD FAILURES OR ERRORS</h1>\n" +
        "<p>Repository:  github</p>\n" +
        "<p>Branch:  new-feature</p>\n" +
        "<p>Profile:  master</p>\n" +
        "<p><b>Counts:  Succeeded:  17, Errors:  1, Failures:  1</b></p>\n" +
        "<p>Links to logfiles for tests with errors, failures, or timeout:</p>\n" +
        "<ul>\n" +
        "<li>TestFakeTwo  <a href=\"reporter-fail\">reporter-fail</a></li>\n" +
        "<li>TestFake  <a href=\"reporter-fail\">reporter-fail</a></li>\n" +
        "</ul>\n" +
        "<p>Logfile from build: <a href=\"dtest.log\">dtest.log</a></p>\n" +
        "<p>Dockerfile used for build: <a href=\"Dockerfile\">Dockerfile</a></p>\n" +
        "</body>\n" +
        "</html>\n", new File(client.getContainerBaseDir(), "index.html"));

    TestUtils.assertFile("<html>\n" +
        "<head>\n" +
        "<title>reporter-fail</title>\n" +
        "</head>\n" +
        "<body>\n" +
        "<h1>Log Files</h1>\n" +
        "<ul>\n" +
        "<li><a href=\"org.dtest.core.TestFakeTwo.txt\">org.dtest.core.TestFakeTwo.txt</a></li>\n" +
        "<li><a href=\"org.dtest.core.TestFake.txt\">org.dtest.core.TestFake.txt</a></li>\n" +
        "</ul>\n" +
        "</body>\n" +
        "</html>\n", new File(client.getContainerBaseDir() + File.separator + containerName, "index.html"));
  }

  @Test
  public void timedout() throws IOException {
    File buildDir = TestUtils.createBuildDir();
    Config cfg = TestUtils.buildCfg();
    String containerName = "reporter-timeout";
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    ResultAnalyzer analyzer = new MavenResultAnalyzer();
    ContainerClient client = new TestUtils.MockContainerClient(containerName, "timeout", buildDir, 0);
    client.setLog(log);
    client.setConfig(cfg);
    client.setBuildInfo(new TestUtils.MockBuildInfo(buildDir));
    ContainerResult cr = client.runContainer(new TestUtils.MockContainerCommand(containerName, buildDir.getAbsolutePath(), "/bin/bash"));
    Reporter reporter = Reporter.getInstance(cfg, log);
    Assert.assertTrue(reporter instanceof HtmlReporter);
    reporter.setRepo("github")
        .setBranch("new-feature")
        .setProfile("master")
        .setBuildInfo(client.buildInfo);
    client.fetchTestReports(cr, analyzer, reporter, null);
    analyzer.analyzeLog(cr);
    reporter.summarize(analyzer);
    reporter.addFailedTests(client, cr);
    reporter.publish();

    TestUtils.assertFile("<html>\n" +
        "<head>\n" +
        "<title>Docker Test</title>\n" +
        "</head>\n" +
        "<body>\n" +
        "<h1>Status:  HAD TIMEOUTS</h1>\n" +
        "<p>Repository:  github</p>\n" +
        "<p>Branch:  new-feature</p>\n" +
        "<p>Profile:  master</p>\n" +
        "<p><b>Counts:  Succeeded:  18, Errors:  0, Failures:  0</b></p>\n" +
        "<p>Logfile from build: <a href=\"dtest.log\">dtest.log</a></p>\n" +
        "<p>Dockerfile used for build: <a href=\"Dockerfile\">Dockerfile</a></p>\n" +
        "</body>\n" +
        "</html>\n", new File(client.getContainerBaseDir(), "index.html"));
  }

}
