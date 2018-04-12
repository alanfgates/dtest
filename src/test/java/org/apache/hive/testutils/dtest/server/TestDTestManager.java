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
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
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
    mgr = new DTestManager(dtest);
    mgr.run();
  }

  @After
  public void stopManager() {
    mgr.close();
  }

  @Test
  public void submitOne() throws InterruptedException {
    BuildInfo build = new BuildInfo("branch", "repo", "label");
    mgr.submitBuild(build);
    // Make sure we've started
    untilNotZero(build::getStartTime, 10000);
    // We may not have been fast enough and the build may have already finished, so don't bork if
    // it comes back null
    BuildInfo current = mgr.getCurrentlyRunningBuild();
    if (current != null) Assert.assertEquals(build, current);

    // Wait until the build has finished
    untilNotZero(build::getCompletionTime, 30000);
    current = mgr.getCurrentlyRunningBuild();
    Assert.assertNull(current);
    Map<BuildInfo, Boolean> finished = mgr.getFinishedBuilds();
    Assert.assertEquals(1, finished.size());
    Assert.assertNotNull(finished.get(build));
    Assert.assertTrue(finished.get(build));
  }

  @Test
  public void submitTwo() throws InterruptedException {
    BuildInfo build1 = new BuildInfo("branch", "repo", "first");
    BuildInfo build2 = new BuildInfo("branch", "repo", "second");
    mgr.submitBuild(build1);
    mgr.submitBuild(build2);
    // Make sure we've started
    untilNotZero(build1::getStartTime, 10000);
    // We may not have been fast enough and the build may have already finished, so don't bork if
    // it comes back null
    Collection<BuildInfo> pending = mgr.getPendingBuilds();
    if (pending.size() > 0) {
      Assert.assertEquals(1, pending.size());
      Assert.assertEquals(build2, pending.iterator().next());
    }
    BuildInfo current = mgr.getCurrentlyRunningBuild();
    if (current != null) Assert.assertEquals(build1, current);

    // Wait until the build has finished
    untilNotZero(build1::getCompletionTime, 30000);
    untilNotZero(build2::getStartTime, 10000);
    current = mgr.getCurrentlyRunningBuild();
    if (current != null) Assert.assertEquals(build2, current);
    untilNotZero(build2::getCompletionTime, 30000);
    Map<BuildInfo, Boolean> finished = mgr.getFinishedBuilds();
    Assert.assertEquals(2, finished.size());
    Assert.assertTrue(finished.get(build1));
    Assert.assertTrue(finished.get(build2));

    mgr.clearSingleBuildHistory(build1);
    finished = mgr.getFinishedBuilds();
    Assert.assertEquals(1, finished.size());
    Assert.assertNull(finished.get(build1));
    Assert.assertTrue(finished.get(build2));

    mgr.clearAllHistory();
    finished = mgr.getFinishedBuilds();
    Assert.assertEquals(0, finished.size());
  }

  @Test
  public void killPending() throws InterruptedException {
    BuildInfo build1 = new BuildInfo("branch", "repo", "first-r2");
    BuildInfo build2 = new BuildInfo("branch", "repo", "second-r2");
    mgr.submitBuild(build1);
    mgr.submitBuild(build2);
    // Make sure we've started
    untilNotZero(build1::getStartTime, 10000);
    mgr.killBuild(build2);
    // Wait until the build has finished
    untilNotZero(build1::getCompletionTime, 30000);
    Map<BuildInfo, Boolean> finished = mgr.getFinishedBuilds();
    Assert.assertEquals(1, finished.size());
    Assert.assertTrue(finished.get(build1));
    Collection<BuildInfo> killed = mgr.getKilledBuilds();
    if (killed.size() > 0) {
      Assert.assertEquals(1, killed.size());
      Assert.assertEquals(build2, killed.iterator().next());
      mgr.clearSingleBuildHistory(build2);
      killed = mgr.getKilledBuilds();
      Assert.assertEquals(0, killed.size());
    }
  }

  @Test
  public void killRunning() throws InterruptedException {
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
    Map<BuildInfo, Boolean> finished = mgr.getFinishedBuilds();
    if (finished.size() == 1) Assert.assertTrue(finished.get(build2));
    Collection<BuildInfo> killed = mgr.getKilledBuilds();
    if (killed.size() > 0) {
      Assert.assertEquals(1, killed.size());
      Assert.assertEquals(build1, killed.iterator().next());
      mgr.clearAllHistory();
      killed = mgr.getKilledBuilds();
      Assert.assertEquals(0, killed.size());
    }
  }

  private void untilNotZero(LongSupplier f, long timeout) throws InterruptedException {
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() < timeout + start && f.getAsLong() == 0) Thread.sleep(100);
  }


}
