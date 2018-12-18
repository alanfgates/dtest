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
import org.dtest.core.Config;
import org.dtest.core.ContainerClient;
import org.dtest.core.ContainerCommand;
import org.dtest.core.ContainerCommandList;
import org.dtest.core.ContainerResult;
import org.dtest.core.DTestLogger;
import org.dtest.core.DockerTest;
import org.dtest.core.ResultAnalyzer;
import org.dtest.core.TestUtils;
import org.dtest.core.git.GitSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestHiveDockerTest {
  private static final Logger LOG = LoggerFactory.getLogger(TestHiveDockerTest.class);
  private static boolean imageBuilt, hadTimeouts, runSucceeded;
  private static int succeeded;
  private static List<String> failures;
  private static List<String> errors;

  private ByteArrayOutputStream outBuffer;
  private PrintStream out;
  private PrintStream err;

  @BeforeClass
  public static void createConfFile() throws IOException {
    TestUtils.createConfFile();
  }

  public static class SuccessfulWithFailingTestsClient extends ContainerClient {
    @Override
    public void defineImage() throws IOException {

    }

    @Override
    public String getProjectName() {
      return null;
    }

    @Override
    public String getContainerBaseDir() {
      return null;
    }

    @Override
    public void buildImage(String dir, DTestLogger logger) throws IOException {
      imageBuilt = true;
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd, DTestLogger logger) throws
        IOException {
      String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
          TestHiveResultAnalyzer.LOG2;
      return new ContainerResult(cmd, 0, logs);
    }

    @Override
    public void copyLogFiles(ContainerResult result, String targetDir, DTestLogger logger) throws IOException {

    }

    @Override
    public void removeContainer(ContainerResult result, DTestLogger logger) throws IOException {

    }

    @Override
    public void removeImage(DTestLogger logger) throws IOException {

    }
  }

  public static class ItestCommandList extends ContainerCommandList {
    @Override
    public void buildContainerCommands(ContainerClient containerClient, BuildInfo label, DTestLogger logger) throws IOException {
      add(new ContainerCommand() {
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
  }

  public static class SpyingResultAnalyzer extends ResultAnalyzer {
    final HiveResultAnalyzer contained = new HiveResultAnalyzer();
    @Override
    public void analyzeLog(ContainerResult result) {
      contained.analyzeLog(result);
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

    @Override
    public boolean hadTimeouts() {
      hadTimeouts = contained.hadTimeouts();
      return hadTimeouts;
    }

    @Override
    public boolean runSucceeded() {
      runSucceeded = contained.runSucceeded();
      return runSucceeded;
    }
  }

  @Before
  public void setup() {
    imageBuilt = false;
    succeeded = 0;
    failures = new ArrayList<>();
    errors = new ArrayList<>();
    hadTimeouts = false;
    outBuffer = new ByteArrayOutputStream();
    out = new PrintStream(outBuffer);
    err = new PrintStream(new ByteArrayOutputStream());
  }

  @Test
  public void successfulRunSomeTestsFail() {
    Config.set(ContainerClient.CFG_CONTAINER_CLIENT, SuccessfulWithFailingTestsClient.class.getName());
    Config.set(ContainerCommandList.CFG_CONTAINER_COMMAND_LIST, ItestCommandList.class.getName());
    Config.set(ResultAnalyzer.CFG_RESULT_ANALYZER, SpyingResultAnalyzer.class.getName());
    Config.set(GitSource.CFG_GIT_BRANCH, "successful");
    Config.set(GitSource.CFG_GIT_REPO, "repo");
    Config.set(BuildInfo.CFG_BUILD_BASE_DIR, System.getProperty("java.io.tmpdir"));
    DockerTest test = new DockerTest(out, err);
    BuildInfo build = test.parseArgs(new String[] {"-l", "secondTry",
                                                   "-c", TestUtils.getConfDir()});
    test.runBuild(build);
    Assert.assertTrue(imageBuilt);
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("TestNegativeCliDriver.alter_notnull_constraint_violation", errors.get(0));
    Assert.assertEquals(1, failures.size());
    Assert.assertEquals("TestNegativeCliDriver.alter_table_constraint_duplicate_pk", failures.get(0));
    Assert.assertEquals(72, succeeded);
    Assert.assertFalse(hadTimeouts);
    Assert.assertTrue(runSucceeded);
    Assert.assertTrue(outBuffer.toString().contains("Test run SUCCEEDED"));
  }

}
