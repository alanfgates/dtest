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

import org.apache.commons.lang3.StringUtils;
import org.dtest.core.mvn.MavenResultAnalyzer;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TestHtmlReporter {

  private static class FakeDocker extends ContainerClient {

    @Override
    public String getContainerBaseDir() {
      /*
      try {
        Path dir = Files.createTempDirectory("TestHtmlReporter");
        return dir.toFile().getAbsolutePath();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      */
      return null;
    }

    @Override
    public void buildImage(ContainerCommandFactory cmdFactory) {

    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd) {
      return null;
    }

    @Override
    public void copyLogFiles(ContainerResult result, String targetDir) throws IOException {
      FileWriter writer = new FileWriter(new File(targetDir, "fake-log-file"));
      writer.write("Here is the result of running " + StringUtils.join(result.getCmd().shellCommand(), " ") + "\n");
      writer.close();

    }

    @Override
    public void removeContainer(ContainerResult result) {

    }

    @Override
    public void removeImage() {

    }
  }

  private static class FakeAnalyzer extends ResultAnalyzer {
    private final int successCount;
    private final List<String> fails;
    private final List<String> errors;

    FakeAnalyzer(BuildState state, int successCount, int failCount, int errorCount) {
      switch (state.getState()) {
        case FAILED:  buildState.fail(); break;
        case HAD_TIMEOUTS: buildState.sawTimeouts(); break;
        case HAD_FAILURES_OR_ERRORS: buildState.sawTestFailureOrError(); break;
        case SUCCEEDED: buildState.success(); break;
        default: throw new RuntimeException("eek!");
      }
      this.successCount = successCount;
      fails = new ArrayList<>(failCount);
      for (int i = 0; i < failCount; i++) fails.add("fail!");
      errors = new ArrayList<>(errorCount);
      for (int i = 0; i < errorCount; i++) errors.add("error!");

    }

    @Override
    public void analyzeLog(ContainerResult containerResult, BuildYaml yaml) {

    }

    @Override
    public int getSucceeded() {
      return successCount;
    }

    @Override
    public List<String> getFailed() {
      return fails;
    }

    @Override
    public List<String> getErrors() {
      return errors;
    }
  }

  private static class FakeBuildInfo extends BuildInfo {
    File fakeBuildDir;

    FakeBuildInfo(File buildDir) {
      super(null, null, false, buildDir.getAbsolutePath());
      fakeBuildDir = buildDir;
    }

    @Override
    public File getBuildDir() {
      return fakeBuildDir;
    }
  }

  private static class FakeContainerCommand extends ContainerCommand {
    private final String cmd;
    private final String suffix;

    FakeContainerCommand(String cmd, String suffix) {
      this.cmd = cmd;
      this.suffix = suffix;
    }

    @Override
    public String containerSuffix() {
      return suffix;
    }

    @Override
    public String[] shellCommand() {
      return new String[] {cmd};
    }

    @Override
    public String containerDirectory() {
      return System.getProperty("java.io.tmpdir");
    }
  }

  @Test
  public void allGood() throws IOException {
    Config cfg = TestUtils.buildCfg();
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Reporter r = Reporter.getInstance(cfg, log);
    Assert.assertTrue(r instanceof HtmlReporter);
    File dir = setupFakeLogfiles(2);
    r.setRepo("github")
        .setBranch("new-feature")
        .setProfile("master")
        .setBuildInfo(new FakeBuildInfo(dir));
    BuildState state = new BuildState();
    state.success();
    r.summarize(new FakeAnalyzer(state, 100, 0, 0));
    r.publish();

    assertFile("<html>\n" +
        "<head>\n" +
        "<title>Docker Test</title>\n" +
        "</head>\n" +
        "<body>\n" +
        "<h1>Status:  SUCCEEDED</h1>\n" +
        "<p>Repository:  github</p>\n" +
        "<p>Branch:  new-feature</p>\n" +
        "<p>Profile:  master</p>\n" +
        "<p>Counts:</p>\n" +
        "<p>Succeeded:  100</p>\n" +
        "<p>Errors:  0</p>\n" +
        "<p>Failures:  0</p>\n" +
        "<p>Logfile from build: <a href=\"dtest.log\">dtest.log</a></p>\n" +
        "<p>Dockerfile used for build: <a href=\"Dockerfile\">Dockerfile</a></p>\n" +
        "</body>\n" +
        "</html>\n", new File(dir, "index.html"));

    assertFile("FROM centos\n", new File(dir, "Dockerfile"));
    assertFile("summary INFO: build the image\n" +
        "unittest-0 INFO ran a test\n" +
        "unittest-1 INFO ran a test\n", new File(dir, "dtest.log"));
  }

  @Test
  public void failedTest() throws IOException {
    Config cfg = TestUtils.buildCfg();
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Reporter r = Reporter.getInstance(cfg, log);
    File dir = setupFakeLogfiles(3);
    r.setRepo("github")
        .setBranch("new-feature2")
        .setProfile("master")
        .setBuildInfo(new FakeBuildInfo(dir));
    ContainerResult result = new ContainerResult(new FakeContainerCommand("ls", "unittest-0"), 1, "/bin /etc /var");
    result.addLogFileToFetch("failing-unit-test", "fake-log-file");
    r.addFailedTests(new FakeDocker(), result);
    BuildState state = new BuildState();
    state.sawTestFailureOrError();
    r.summarize(new FakeAnalyzer(state, 99, 1, 0));
    r.publish();

    assertFile("<html>\n" +
        "<head>\n" +
        "<title>Docker Test</title>\n" +
        "</head>\n" +
        "<body>\n" +
        "<h1>Status:  HAD FAILURES OR ERRORS</h1>\n" +
        "<p>Repository:  github</p>\n" +
        "<p>Branch:  new-feature2</p>\n" +
        "<p>Profile:  master</p>\n" +
        "<p>Counts:</p>\n" +
        "<p>Succeeded:  99</p>\n" +
        "<p>Errors:  0</p>\n" +
        "<p>Failures:  1</p>\n" +
        "<p>Links to logfiles for tests with errors, failures, or timeout:</p>\n" +
        "<ul>\n" +
        "<li>failing-unit-test  <a href=\"unittest-0\">unittest-0</a></li>\n" +
        "</ul>\n" +
        "<p>Logfile from build: <a href=\"dtest.log\">dtest.log</a></p>\n" +
        "<p>Dockerfile used for build: <a href=\"Dockerfile\">Dockerfile</a></p>\n" +
        "</body>\n" +
        "</html>\n", new File(dir, "index.html"));
    File dir0 = new File(dir, "unittest-0");
    Assert.assertTrue(dir0.exists() && dir0.isDirectory());
    assertFile("<html>\n" +
        "<head>\n" +
        "<title>unittest-0</title>\n" +
        "</head>\n" +
        "<body>\n" +
        "<h1>Log Files</h1>\n" +
        "<ul>\n" +
        "<li><a href=\"fake-log-file\">fake-log-file</a></li>\n" +
        "</ul>\n" +
        "<p>Section of logfile generated by this container: <a href=\"dtest.log\">dtest.log</a></p></body>\n" +
        "</html>\n", new File(dir0, "index.html"));
    assertFile("Here is the result of running ls\n", new File(dir0, "fake-log-file"));
    assertFile("unittest-0 INFO ran a test\n", new File(dir0, "dtest.log"));
  }

  @Test
  public void timedout() throws IOException {
    Config cfg = TestUtils.buildCfg();
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Reporter r = Reporter.getInstance(cfg, log);
    File dir = setupFakeLogfiles(3);
    r.setRepo("github")
        .setBranch("new-feature3")
        .setProfile("master")
        .setBuildInfo(new FakeBuildInfo(dir));
    ContainerResult result = new ContainerResult(new FakeContainerCommand("ls", "unittest-1"), 0, null);
    result.addLogFileToFetch(MavenResultAnalyzer.TIMED_OUT_KEY, "fake-log-file");
    r.addFailedTests(new FakeDocker(), result);
    BuildState state = new BuildState();
    state.sawTimeouts();
    r.summarize(new FakeAnalyzer(state, 99, 0, 0));
    r.publish();

    assertFile("<html>\n" +
        "<head>\n" +
        "<title>Docker Test</title>\n" +
        "</head>\n" +
        "<body>\n" +
        "<h1>Status:  HAD TIMEOUTS</h1>\n" +
        "<p>Repository:  github</p>\n" +
        "<p>Branch:  new-feature3</p>\n" +
        "<p>Profile:  master</p>\n" +
        "<p>Counts:</p>\n" +
        "<p>Succeeded:  99</p>\n" +
        "<p>Errors:  0</p>\n" +
        "<p>Failures:  0</p>\n" +
        "<p>Links to logfiles for tests with errors, failures, or timeout:</p>\n" +
        "<ul>\n" +
        "<li>unittest-1 timed out  <a href=\"unittest-1\">unittest-1</a></li>\n" +
        "</ul>\n" +
        "<p>Logfile from build: <a href=\"dtest.log\">dtest.log</a></p>\n" +
        "<p>Dockerfile used for build: <a href=\"Dockerfile\">Dockerfile</a></p>\n" +
        "</body>\n" +
        "</html>\n", new File(dir, "index.html"));
    File dir1 = new File(dir, "unittest-1");
    Assert.assertTrue(dir1.exists() && dir1.isDirectory());
    assertFile("<html>\n" +
        "<head>\n" +
        "<title>unittest-1</title>\n" +
        "</head>\n" +
        "<body>\n" +
        "<h1>Log Files</h1>\n" +
        "<ul>\n" +
        "<li><a href=\"fake-log-file\">fake-log-file</a></li>\n" +
        "</ul>\n" +
        "<p>Section of logfile generated by this container: <a href=\"dtest.log\">dtest.log</a></p></body>\n" +
        "</html>\n", new File(dir1, "index.html"));
    assertFile("Here is the result of running ls\n", new File(dir1, "fake-log-file"));
    assertFile("unittest-1 INFO ran a test\n", new File(dir1, "dtest.log"));
  }

  private File setupFakeLogfiles(int numContainers) throws IOException {
    File dir = Files.createTempDirectory("TestHtmlReporter").toFile();
    FileWriter writer = new FileWriter(new File(dir, "Dockerfile"));
    writer.write("FROM centos\n");
    writer.close();
    writer = new FileWriter(new File(dir, "dtest.log"));
    writer.write("summary INFO: build the image\n");
    for (int i = 0; i < numContainers; i++) writer.write("unittest-" + i + " INFO ran a test\n");
    writer.close();
    return dir;
  }

  private void assertFile(String expected, File file) throws IOException {
    Assert.assertTrue(file.exists());
    StringBuilder buf = new StringBuilder();
    BufferedReader reader = new BufferedReader(new FileReader(file));
    reader.lines().forEach(s -> buf.append(s).append('\n'));
    reader.close();
    Assert.assertEquals(expected, buf.toString());
  }
}
