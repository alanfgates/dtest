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
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class TestDockerTest {
  private static boolean imageBuilt;
  private static int succeeded;
  private static List<String> failures;
  private static List<String> errors;
  private static File buildDir;

  public static class SuccessfulClient extends TestUtils.MockContainerClient {
    static final String CONTAINER_NAME = "successful";
    static final int CONTAINER_RC = 0;

    public SuccessfulClient() throws IOException {
      super(CONTAINER_NAME, "allgood", buildDir, CONTAINER_RC);
    }

    @Override
    public void buildImage(ContainerCommandFactory cmdFactory) throws IOException {
      buildInfo.getBuildDir();
      imageBuilt = true;
    }
  }

  public static class TimingOutClient extends TestUtils.MockContainerClient {
    static final String CONTAINER_NAME = "timing-out";
    static final int CONTAINER_RC = 0;

    public TimingOutClient() throws IOException {
      super(CONTAINER_NAME, "timeout", buildDir, CONTAINER_RC);
    }

    @Override
    public void buildImage(ContainerCommandFactory cmdFactory) throws IOException {
      buildInfo.getBuildDir();
      imageBuilt = true;
    }
  }

  public static class ClientWithFailures extends TestUtils.MockContainerClient {
    static final String CONTAINER_NAME = "has-issues";
    static final int CONTAINER_RC = 0;

    public ClientWithFailures() throws IOException {
      super(CONTAINER_NAME, "with-error-and-failure", buildDir, CONTAINER_RC);
    }


    @Override
    public void buildImage(ContainerCommandFactory cmdFactory) throws IOException {
      buildInfo.getBuildDir();
      imageBuilt = true;
    }
  }


  public static class HelloWorldCommandList extends TestUtils.MockContainerCommandFactory {

    public HelloWorldCommandList() {
      super(Collections.singletonList(new ContainerCommand(new ModuleDirectory()) {
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
          return buildDir.getAbsolutePath();
        }
      }));
    }
  }

  public static class FailureIgnoringCommandList extends TestUtils.MockContainerCommandFactory {
    public FailureIgnoringCommandList() {
      super(Collections.singletonList(new ContainerCommand(new ModuleDirectory().setFailuresToIgnore(new String[] {"TestFakeTwo.errorTwo", "TestFake.fail"})) {
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
          return buildDir.getAbsolutePath();
        }
      }));
    }
  }

public static class SpyingResultAnalyzer extends ResultAnalyzer {
    final MavenResultAnalyzer wrapped;

    public SpyingResultAnalyzer() {
      wrapped = new MavenResultAnalyzer();
    }


    @Override
    public Configurable setConfig(Config cfg) {
      super.setConfig(cfg);
      wrapped.setConfig(cfg);
      return this;
    }

    @Override
    public Configurable setLog(DTestLogger log) {
      super.setLog(log);
      wrapped.setLog(log);
      return this;
    }

    @Override
    public void analyzeResult(ContainerResult containerResult, ContainerCommand cmd) throws IOException {
      wrapped.analyzeResult(containerResult, cmd);
    }

    @Override
    public String getTestResultsDir() {
      return wrapped.getTestResultsDir();
    }

    @Override
    public int getSucceeded() {
      succeeded = wrapped.getSucceeded();
      return succeeded;
    }

    @Override
    public List<String> getFailed() {
      failures = wrapped.getFailed();
      return failures;
    }

    @Override
    public List<String> getErrors() {
      errors = wrapped.getErrors();
      return errors;
    }

    @Override
    public BuildState getBuildState() {
      // Return the wrapped build state rather than our own
      log.debug("Going to return build state of " + wrapped.getBuildState().getState());
      return wrapped.getBuildState();
    }
  }

  @Before
  public void setup() throws IOException {
    imageBuilt = false;
    succeeded = 0;
    failures = new ArrayList<>();
    errors = new ArrayList<>();
    buildDir = TestUtils.createBuildDir();
  }

  @Test
  public void successfulRunAllTestsPass() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    try {
      Properties props = TestUtils.buildProperties(
          ContainerClient.CFG_CONTAINERCLIENT_IMPL, SuccessfulClient.class.getName(),
          ContainerCommandFactory.CFG_CONTAINERCOMMANDLIST_IMPL, HelloWorldCommandList.class.getName(),
          ResultAnalyzer.CFG_RESULTANALYZER_IMPL, SpyingResultAnalyzer.class.getName(),
          BuildInfo.CFG_BUILDINFO_BASEDIR, TestUtils.getConfDir().getAbsolutePath(),
          BuildInfo.CFG_BUILDINFO_LABEL, "firsttry");
      DockerTest test = TestUtils.getAndPrepDockerTest(props, log);
      BuildState state = test.runBuild();
      Assert.assertEquals(BuildState.State.SUCCEEDED, state.getState());
      Assert.assertTrue(imageBuilt);
      Assert.assertEquals(0, errors.size());
      Assert.assertEquals(0, failures.size());
      Assert.assertEquals(17, succeeded);
      Assert.assertTrue(log.toString().contains("SUCCEEDED, the build ran to completion and all tests passed"));
    } finally {
      log.dumpToLog();
    }
  }

  @Test
  public void ignoreFailures() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    try {
      Properties props = TestUtils.buildProperties(
          ContainerClient.CFG_CONTAINERCLIENT_IMPL, SuccessfulClient.class.getName(),
          ContainerCommandFactory.CFG_CONTAINERCOMMANDLIST_IMPL, FailureIgnoringCommandList.class.getName(),
          ResultAnalyzer.CFG_RESULTANALYZER_IMPL, SpyingResultAnalyzer.class.getName(),
          BuildInfo.CFG_BUILDINFO_BASEDIR, TestUtils.getConfDir().getAbsolutePath(),
          BuildInfo.CFG_BUILDINFO_LABEL, "firsttry");
      DockerTest test = TestUtils.getAndPrepDockerTest(props, log);
      BuildState state = test.runBuild();
      Assert.assertEquals(BuildState.State.SUCCEEDED, state.getState());
      Assert.assertTrue(imageBuilt);
      Assert.assertEquals(0, errors.size());
      Assert.assertEquals(0, failures.size());
      Assert.assertEquals(17, succeeded);
      Assert.assertTrue(log.toString().contains("SUCCEEDED, the build ran to completion and all tests passed"));
    } finally {
      log.dumpToLog();
    }
  }

  @Test
  public void timeout() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Properties props = TestUtils.buildProperties(
        ContainerClient.CFG_CONTAINERCLIENT_IMPL, TimingOutClient.class.getName(),
        ContainerCommandFactory.CFG_CONTAINERCOMMANDLIST_IMPL, HelloWorldCommandList.class.getName(),
        ResultAnalyzer.CFG_RESULTANALYZER_IMPL, SpyingResultAnalyzer.class.getName(),
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildInfo.CFG_BUILDINFO_LABEL, "will-time-out");
    DockerTest test = TestUtils.getAndPrepDockerTest(props, log);
    BuildState state = test.runBuild();
    log.dumpToLog();
    Assert.assertEquals(BuildState.State.HAD_TIMEOUTS, state.getState());
    Assert.assertTrue(imageBuilt);
    Assert.assertTrue(log.toString().contains("HAD TIMEOUTS, the build ran to completion but some containers timed out"));
  }

  @Test
  public void runWithFailures() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Properties props = TestUtils.buildProperties(
        ContainerClient.CFG_CONTAINERCLIENT_IMPL, ClientWithFailures.class.getName(),
        ContainerCommandFactory.CFG_CONTAINERCOMMANDLIST_IMPL, HelloWorldCommandList.class.getName(),
        ResultAnalyzer.CFG_RESULTANALYZER_IMPL, SpyingResultAnalyzer.class.getName(),
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildInfo.CFG_BUILDINFO_LABEL, "take2");
    DockerTest test = TestUtils.getAndPrepDockerTest(props, log);
    BuildState state = test.runBuild();
    log.dumpToLog();
    Assert.assertEquals(BuildState.State.HAD_FAILURES_OR_ERRORS, state.getState());
    Assert.assertTrue(imageBuilt);
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals(1, failures.size());
    Assert.assertEquals(17, succeeded);
    Assert.assertTrue(log.toString().contains("HAD FAILURES OR ERRORS, the build ran to completion but some tests failed or had errors"));
  }

  @Test
  public void cmdline() {
    DockerTest test = new DockerTest();
    Assert.assertTrue(test.parseArgs(new String[]{"-p", "profile", "-n", "-d", "/tmp"}));
    Assert.assertFalse(test.isCleanupAfter());
  }

}
