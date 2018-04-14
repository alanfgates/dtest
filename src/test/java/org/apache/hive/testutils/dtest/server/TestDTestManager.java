/*
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
package org.apache.hive.testutils.dtest.server;

import org.apache.commons.lang3.StringUtils;
import org.apache.hive.testutils.dtest.BuildInfo;
import org.apache.hive.testutils.dtest.BuildState;
import org.apache.hive.testutils.dtest.ContainerClient;
import org.apache.hive.testutils.dtest.ContainerClientFactory;
import org.apache.hive.testutils.dtest.ContainerCommand;
import org.apache.hive.testutils.dtest.ContainerCommandFactory;
import org.apache.hive.testutils.dtest.DockerTest;
import org.apache.hive.testutils.dtest.ResultAnalyzer;
import org.apache.hive.testutils.dtest.ResultAnalyzerFactory;
import org.apache.hive.testutils.dtest.impl.ContainerResult;
import org.apache.hive.testutils.dtest.impl.DTestLogger;
import org.apache.hive.testutils.dtest.impl.TestSimpleResultAnalyzer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public class TestDTestManager {
  private static final Logger LOG = LoggerFactory.getLogger(TestDTestManager.class);

  private DockerTest dtest;
  private ByteArrayOutputStream outBuffer;
  private PrintStream out;
  private PrintStream err;
  private DTestManager mgr;

  public static class PausingContainerFactory extends ContainerClientFactory {
    @Override
    public ContainerClient getClient(String label) {
      return new ContainerClient() {
        @Override
        public void buildImage(String dir, long toWait, TimeUnit unit, DTestLogger logger) throws IOException {
        }

        @Override
        public ContainerResult runContainer(long toWait, TimeUnit unit, ContainerCommand cmd, DTestLogger logger) throws IOException {
          String logs = "Ran: " + StringUtils.join(cmd.shellCommand(), " ") +
              TestSimpleResultAnalyzer.LOG1;
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            // oh well
          }
          return new ContainerResult(cmd.containerName(), 0, logs);
        }
      };
    }
  }

  public static class MyCommandFactory extends ContainerCommandFactory {
    @Override
    public List<ContainerCommand> getContainerCommands(String baseDir) throws IOException {
      return Collections.singletonList(new ContainerCommand() {
        @Override
        public String containerName() {
          return "friendly";
        }

        @Override
        public String[] shellCommand() {
          return new String[] {"echo", "hello", "world"};
        }
      });
    }
  }

  public static class MyResultAnalyzerFactory extends ResultAnalyzerFactory {
    @Override
    public ResultAnalyzer getAnalyzer() {
      return new ResultAnalyzer() {
        @Override
        public void analyzeLog(ContainerResult result) {
        }

        @Override
        public int getSucceeded() {
          return 1;
        }

        @Override
        public List<String> getFailed() {
          return Collections.emptyList();
        }

        @Override
        public List<String> getErrors() {
          return Collections.emptyList();
        }

        @Override
        public boolean hadTimeouts() {
          return false;
        }

        @Override
        public boolean runSucceeded() {
          return true;
        }
      };
    }
  }

  @Before
  public void createDTest() {
    outBuffer = new ByteArrayOutputStream();
    out = new PrintStream(outBuffer);
    err = new PrintStream(new ByteArrayOutputStream());
    dtest = new DockerTest(out, err);
    dtest.parseArgs(new String[] {"-C", MyCommandFactory.class.getName(),
                                  "-d", System.getProperty("java.io.tmpdir"),
                                  "-F", PausingContainerFactory.class.getName(),
                                  "-R", MyResultAnalyzerFactory.class.getName(),
                                  "-s"});
    DTestManager.initialize(dtest);
    mgr = DTestManager.get();
  }

  @After
  public void stopManager() {
    if (mgr != null) {
      mgr.close();
      DTestManager.resetForTesting();
    }
  }

  @Test
  public void submitOne() throws InterruptedException, IOException {
    BuildInfo build = new BuildInfo("branch", "repo", "label");
    mgr.submitBuild(build);
    // Make sure we've started
    untilNotZero(build::getStartTime, 10000);
    // We may not have been fast enough and the build may have already finished, so don't bork if
    // it comes back null
    Map<String, BuildState> status = mgr.getFullState();
    Assert.assertEquals(1, status.size());
    Assert.assertTrue(status.get(build.getLabel()) == BuildState.BUILDING ||
        status.get(build.getLabel()) == BuildState.SUCCEEDED);

    // Wait until the build has finished
    untilNotZero(build::getCompletionTime, 30000);
    status = mgr.getFullState();
    Assert.assertEquals(1, status.size());
    Assert.assertEquals(BuildState.SUCCEEDED, status.get(build.getLabel()));
  }

  @Test
  public void submitTwo() throws InterruptedException, IOException {
    BuildInfo build1 = new BuildInfo("branch", "repo", "first");
    BuildInfo build2 = new BuildInfo("branch", "repo", "second");
    mgr.submitBuild(build1);
    mgr.submitBuild(build2);
    // Make sure we've started
    untilNotZero(build1::getStartTime, 10000);
    // We may not have been fast enough and the build may have already finished, so don't bork if
    // it comes back null
    Map<String, BuildState> status = mgr.getFullState();
    Assert.assertEquals(2, status.size());
    if (status.get(build1.getLabel()) == BuildState.BUILDING) {
      Assert.assertEquals(BuildState.PENDING, status.get(build2.getLabel()));
    }

    // Wait until the build has finished
    untilNotZero(build1::getCompletionTime, 30000);
    untilNotZero(build2::getStartTime, 10000);
    status = mgr.getFullState();
    Assert.assertEquals(2, status.size());
    Assert.assertEquals(BuildState.SUCCEEDED, status.get(build1.getLabel()));
    untilNotZero(build2::getCompletionTime, 30000);

    mgr.clearSingleBuildHistory(build1);
    status = mgr.getFullState();
    Assert.assertEquals(1, status.size());
    Assert.assertNotNull(status.get(build2.getLabel()));

    mgr.clearAllHistory();
    status = mgr.getFullState();
    Assert.assertEquals(0, status.size());
  }

  @Test
  public void killPending() throws InterruptedException, IOException {
    BuildInfo build1 = new BuildInfo("branch", "repo", "first-r2");
    BuildInfo build2 = new BuildInfo("branch", "repo", "second-r2");
    mgr.submitBuild(build1);
    mgr.submitBuild(build2);
    // Make sure we've started
    untilNotZero(build1::getStartTime, 10000);
    mgr.killBuild(build2);
    // Wait until the build has finished
    untilNotZero(build1::getCompletionTime, 30000);
    Map<String, BuildState> buildStates = mgr.getFullState();
    // We might have kill build2
    if (build2.isKilled()) {
      Assert.assertEquals(BuildState.KILLED, buildStates.get(build2.getLabel()));
    }
  }

  @Test
  public void killRunning() throws InterruptedException, IOException {
    BuildInfo build1 = new BuildInfo("branch", "repo", "first-r2");
    BuildInfo build2 = new BuildInfo("branch", "repo", "second-r2");
    mgr.submitBuild(build1);
    mgr.submitBuild(build2);
    // Make sure we've started
    untilNotZero(build1::getStartTime, 10000);
    // This is probabilistic.  It may finish before we manage to kill it
    mgr.killBuild(build1);
    // Wait until the build has finished
    untilNotZero(build2::getCompletionTime, 30000);
    Map<String, BuildState> buildStates = mgr.getFullState();
    // We might have kill build1
    if (build1.isKilled()) {
      Assert.assertEquals(BuildState.KILLED, buildStates.get(build1.getLabel()));
    }
  }

  private void untilNotZero(LongSupplier f, long timeout) throws InterruptedException {
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() < timeout + start && f.getAsLong() == 0) Thread.sleep(100);
  }


}
