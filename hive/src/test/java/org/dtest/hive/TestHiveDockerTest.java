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

import org.apache.commons.lang3.StringUtils;
import org.dtest.core.BuildInfo;
import org.dtest.core.BuildState;
import org.dtest.core.CodeSource;
import org.dtest.core.Config;
import org.dtest.core.Configurable;
import org.dtest.core.ContainerClient;
import org.dtest.core.ContainerCommand;
import org.dtest.core.ContainerCommandFactory;
import org.dtest.core.ContainerResult;
import org.dtest.core.DTestLogger;
import org.dtest.core.DockerTest;
import org.dtest.core.ResultAnalyzer;
import org.dtest.core.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TestHiveDockerTest {
  private static final Logger LOG = LoggerFactory.getLogger(TestHiveDockerTest.class);
  private static boolean imageBuilt;
  private static int succeeded;
  private static List<String> failures;
  private static List<String> errors;

  public static class SuccessfulWithFailingTestsClient extends ContainerClient {
    @Override
    public String getProjectName() {
      return null;
    }

    @Override
    public String getContainerBaseDir() {
      return null;
    }

    @Override
    public void buildImage(ContainerCommandFactory cmdFactory) throws IOException {
      imageBuilt = true;
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd) throws
        IOException {
      String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
          TestHiveResultAnalyzer.LOG_QFILE_WITH_FAILURES;
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

  public static class ItestCommandList extends ContainerCommandFactory {
    @Override
    public void buildContainerCommands(ContainerClient containerClient, BuildInfo label) throws IOException {
      getCmds().add(new ContainerCommand() {
        @Override
        public String containerSuffix() {
          return "friendly-itests-qtest";
        }

        @Override
        public String[] shellCommand() {
          return new String[] {"echo", "hello", "world"};
        }

        @Override
        public String containerDirectory() {
          return "/tmp";
        }
      });
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
    final HiveResultAnalyzer contained = new HiveResultAnalyzer();
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
    public void analyzeLog(ContainerResult result) {
      contained.analyzeLog(result);
      buildState = contained.getBuildState();
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
  public void successfulRunSomeTestsFail() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Properties props = TestUtils.buildProperties(
        ContainerClient.CFG_CONTAINERCLIENT_IMPL, SuccessfulWithFailingTestsClient.class.getName(),
        ContainerCommandFactory.CFG_CONTAINERCOMMANDLIST_IMPL, ItestCommandList.class.getName(),
        ResultAnalyzer.CFG_RESULTANALYZER_IMPL, SpyingResultAnalyzer.class.getName(),
        CodeSource.CFG_CODESOURCE_BRANCH, "successful",
        CodeSource.CFG_CODESOURCE_REPO, "repo",
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildInfo.CFG_BUILDINFO_LABEL, "secondTry");
    DockerTest test = new DockerTest();
    test.buildConfig(props);
    test.setLogger(log);
    BuildState state = test.runBuild();
    Assert.assertTrue(imageBuilt);
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("TestNegativeCliDriver.alter_notnull_constraint_violation", errors.get(0));
    Assert.assertEquals(1, failures.size());
    Assert.assertEquals("TestNegativeCliDriver.alter_table_constraint_duplicate_pk", failures.get(0));
    Assert.assertEquals(72, succeeded);
    Assert.assertEquals(BuildState.State.HAD_FAILURES_OR_ERRORS, state.getState());
    Assert.assertTrue(log.toString().contains("HAD FAILURES OR ERRORS"));
  }

}
