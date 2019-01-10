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
import org.dtest.core.mvn.TestMavenResultAnalyzer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TestDockerTest {
  private static boolean imageBuilt;
  private static int succeeded;
  private static List<String> failures;
  private static List<String> errors;
  private static String logToReturn;

  public static class SuccessfulClient extends ContainerClient {

    @Override
    public String getContainerBaseDir() {
      return null;
    }

    @Override
    public void buildImage(ContainerCommandFactory cmdFactory) throws IOException {
      buildInfo.getBuildDir();
      imageBuilt = true;
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd) throws IOException {
      String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") + logToReturn;
          //TestMavenResultAnalyzer.LOG_SUCCESSFUL_RUN_FAILED_TESTS;
      return new ContainerResult(cmd, 0, logs);
    }

    @Override
    public void copyLogFiles(ContainerResult result, String targetDir) throws IOException {

    }

    @Override
    public void removeContainer(ContainerResult result) throws IOException {

    }

    @Override
    public void removeImage() throws IOException {

    }
  }

  public static class TimingOutClient extends ContainerClient {
    @Override
    public String getContainerBaseDir() {
      return null;
    }

    @Override
    public void buildImage(ContainerCommandFactory cmdFactory) throws IOException {
      buildInfo.getBuildDir();
      imageBuilt = true;
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd) throws
        IOException {
      String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
          TestMavenResultAnalyzer.LOG_TIMED_OUT;
      return new ContainerResult(cmd, 0, logs);
    }

    @Override
    public void copyLogFiles(ContainerResult result, String targetDir) throws IOException {

    }

    @Override
    public void removeContainer(ContainerResult result) throws IOException {

    }

    @Override
    public void removeImage() throws IOException {

    }
  }

  public static class FailingClient extends ContainerClient {
    @Override
    public String getContainerBaseDir() {
      return null;
    }

    @Override
    public void buildImage(ContainerCommandFactory cmdFactory) throws IOException {
      buildInfo.getBuildDir();
      imageBuilt = true;
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd) throws
        IOException {
      String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
          TestMavenResultAnalyzer.LOG_SUCCESSFUL_RUN_FAILED_TESTS;
      //return new ContainerResult(cmd, 130, logs);
      throw new IOException("Help me!");
    }

    @Override
    public void copyLogFiles(ContainerResult result, String targetDir) throws IOException {

    }

    @Override
    public void removeContainer(ContainerResult result) throws IOException {

    }

    @Override
    public void removeImage() throws IOException {

    }
  }


  public static class HelloWorldCommandList extends ContainerCommandFactory {
    @Override
    public void buildContainerCommands(ContainerClient containerClient, BuildInfo label) throws IOException {
      ContainerCommand cmd = new ContainerCommand() {
        @Override
        public String containerSuffix() {
          return "friendly";
        }

        @Override
        public String[] shellCommand() {
          return new String[] {"echo", "hello", "world"};
        }

        @Override
        public String containerDirectory() {
          return "/tmp";
        }
      };
      cmd.setConfig(cfg).setLog(log);
      cmds.add(cmd);
    }

    @Override
    public List<String> getInitialBuildCommand() {
      return null;
    }

    @Override
    public List<String> getRequiredPackages() {
      return null;
    }
  }

  public static class SpyingResultAnalyzer extends ResultAnalyzer {
    MavenResultAnalyzer contained = new MavenResultAnalyzer();

    @Override
    public Configurable setConfig(Config cfg) {
      super.setConfig(cfg);
      contained.setConfig(cfg);
      return this;
    }

    @Override
    public Configurable setLog(DTestLogger log) {
      super.setLog(log);
      contained.setLog(log);
      return this;
    }

    @Override
    public void analyzeLog(ContainerResult result, BuildYaml yaml) throws IOException {
      contained.analyzeLog(result, yaml);
      buildState = contained.buildState;
    }

    @Override
    public int getSucceeded() {
      succeeded = contained.getSucceeded();
      return succeeded;
    }

    @Override
    public List<String> getFailed() {
      failures = contained.getFailed();
      return failures;
    }

    @Override
    public List<String> getErrors() {
      errors = contained.getErrors();
      return errors;
    }
  }

  @Before
  public void setup() {
    imageBuilt = false;
    succeeded = 0;
    failures = new ArrayList<>();
    errors = new ArrayList<>();
  }

  @Test
  public void successfulRunAllTestsPass() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Properties props = TestUtils.buildProperties(
        ContainerClient.CFG_CONTAINERCLIENT_IMPL, SuccessfulClient.class.getName(),
        ContainerCommandFactory.CFG_CONTAINERCOMMANDLIST_IMPL, HelloWorldCommandList.class.getName(),
        ResultAnalyzer.CFG_RESULTANALYZER_IMPL, SpyingResultAnalyzer.class.getName(),
        CodeSource.CFG_CODESOURCE_BRANCH, "successful",
        BuildInfo.CFG_BUILDINFO_BASEDIR, TestUtils.getConfDir(),
        BuildInfo.CFG_BUILDINFO_LABEL, "firsttry");
    logToReturn = TestMavenResultAnalyzer.LOG_SUCCESSFUL_RUN_ALL_SUCCEEDED;
    DockerTest test = new DockerTest();
    test.buildConfig(TestUtils.getConfDir(), props);
    test.setLogger(log);
    BuildState state = test.runBuild();
    log.dumpToLog();
    Assert.assertEquals(BuildState.State.SUCCEEDED, state.getState());
    Assert.assertTrue(imageBuilt);
    Assert.assertEquals(0, errors.size());
    Assert.assertEquals(0, failures.size());
    Assert.assertEquals(19, succeeded);
    Assert.assertTrue(log.toString().contains("SUCCEEDED, the build ran to completion and all tests passed"));
  }

  @Test
  public void successfulRunSomeTestsFail() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Properties props = TestUtils.buildProperties(
        ContainerClient.CFG_CONTAINERCLIENT_IMPL, SuccessfulClient.class.getName(),
        ContainerCommandFactory.CFG_CONTAINERCOMMANDLIST_IMPL, HelloWorldCommandList.class.getName(),
        ResultAnalyzer.CFG_RESULTANALYZER_IMPL, SpyingResultAnalyzer.class.getName(),
        CodeSource.CFG_CODESOURCE_BRANCH, "successful",
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildInfo.CFG_BUILDINFO_LABEL, "firsttry");
    logToReturn = TestMavenResultAnalyzer.LOG_SUCCESSFUL_RUN_FAILED_TESTS;
    DockerTest test = new DockerTest();
    test.buildConfig(TestUtils.getConfDir(), props);
    test.setLogger(log);
    BuildState state = test.runBuild();
    log.dumpToLog();
    Assert.assertEquals(BuildState.State.HAD_FAILURES_OR_ERRORS, state.getState());
    Assert.assertTrue(imageBuilt);
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("TestAcidOnTez.testGetSplitsLocks", errors.get(0));
    Assert.assertEquals(1, failures.size());
    Assert.assertEquals("TestActivePassiveHA.testManualFailover", failures.get(0));
    Assert.assertEquals(32, succeeded);
    Assert.assertTrue(log.toString().contains("HAD FAILURES OR ERRORS"));
  }

  @Test
  public void timeout() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Properties props = TestUtils.buildProperties(
        ContainerClient.CFG_CONTAINERCLIENT_IMPL, TimingOutClient.class.getName(),
        ContainerCommandFactory.CFG_CONTAINERCOMMANDLIST_IMPL, HelloWorldCommandList.class.getName(),
        ResultAnalyzer.CFG_RESULTANALYZER_IMPL, SpyingResultAnalyzer.class.getName(),
        CodeSource.CFG_CODESOURCE_BRANCH, "failure",
        CodeSource.CFG_CODESOURCE_REPO, "repo",
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildInfo.CFG_BUILDINFO_LABEL, "will-time-out");
    DockerTest test = new DockerTest();
    test.buildConfig(TestUtils.getConfDir(), props);
    test.setLogger(log);
    BuildState state = test.runBuild();
    log.dumpToLog();
    Assert.assertEquals(BuildState.State.HAD_TIMEOUTS, state.getState());
    Assert.assertTrue(imageBuilt);
    Assert.assertTrue(log.toString().contains("HAD TIMEOUTS, the build ran to completion but some containers timed out"));
  }

  @Test
  public void failedRun() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Properties props = TestUtils.buildProperties(
        ContainerClient.CFG_CONTAINERCLIENT_IMPL, FailingClient.class.getName(),
        ContainerCommandFactory.CFG_CONTAINERCOMMANDLIST_IMPL, HelloWorldCommandList.class.getName(),
        ResultAnalyzer.CFG_RESULTANALYZER_IMPL, SpyingResultAnalyzer.class.getName(),
        CodeSource.CFG_CODESOURCE_BRANCH, "failure",
        CodeSource.CFG_CODESOURCE_REPO, "repo",
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildInfo.CFG_BUILDINFO_LABEL, "take2");
    DockerTest test = new DockerTest();
    test.buildConfig(TestUtils.getConfDir(), props);
    test.setLogger(log);
    BuildState state = test.runBuild();
    log.dumpToLog();
    Assert.assertEquals(BuildState.State.FAILED, state.getState());
    Assert.assertTrue(imageBuilt);
    Assert.assertTrue(log.toString().contains("FAILED, the build did not run to completion"));
  }

  @Test
  public void cmdline() {
    DockerTest test = new DockerTest();
    Assert.assertTrue(test.parseArgs(new String[]{"-c", "cfgdir", "-n", "-D", "key1=val1", "-D", "key2=val2"}));
    Assert.assertEquals("cfgdir", test.getCfgDir());
    Assert.assertTrue(test.isCleanupAfter());
    Assert.assertEquals("val2", test.getCmdLineProps().getProperty("key2"));
    Assert.assertEquals("val1", test.getCmdLineProps().getProperty("key1"));
  }

}
