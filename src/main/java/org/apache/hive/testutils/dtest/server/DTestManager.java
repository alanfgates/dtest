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

import com.google.common.annotations.VisibleForTesting;
import org.apache.hive.testutils.dtest.BuildInfo;
import org.apache.hive.testutils.dtest.BuildState;
import org.apache.hive.testutils.dtest.DockerTest;
import org.apache.hive.testutils.dtest.impl.DTestLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DTestManager {
  private static final Logger LOG = LoggerFactory.getLogger(DTestManager.class);
  private static final int READ_BUF_SZ = 10240;

  private static DTestManager self = null;

  private final DockerTest dtest;
  private Lock lock;
  private ExecutorService executor;
  private ScheduledExecutorService buildCheckerPool;
  private Map<BuildInfo, Future<Boolean>> pendingAndRunningBuilds;
  private Map<String, BuildInfo> allTrackedBuilds;

  private DTestManager(DockerTest dtest) {
    this.dtest = dtest;
    lock = new ReentrantLock();
    LOG.info("Starting the test manager");
    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, queue);
    buildCheckerPool = new ScheduledThreadPoolExecutor(1);
    buildCheckerPool.scheduleAtFixedRate(buildChecker, 500, 1000, TimeUnit.MILLISECONDS);
    pendingAndRunningBuilds = new HashMap<>();
    allTrackedBuilds = new HashMap<>();
  }


  /**
   * This should only be called by DockerTest when it is setting up the server
   * @param dtest DockerTest instance
   */
  static void initialize(DockerTest dtest) {
    assert self == null;
    self = new DTestManager(dtest);
  }

  public static DTestManager get() {
    assert self != null;
    return self;
  }

  private Runnable buildChecker = new Runnable() {
    @Override
    public void run() {
      LOG.debug("checking builds to determine current state");
      List<BuildInfo> toBeRemoved = new ArrayList<>();
      lock.lock();
      try {
        for (Map.Entry<BuildInfo, Future<Boolean>> entry : pendingAndRunningBuilds.entrySet()) {
          BuildInfo info = entry.getKey();
          if (entry.getValue().isDone()) {
            info.setCompletionTime(System.currentTimeMillis());
            toBeRemoved.add(info);
            try {
              info.setSuccess(entry.getValue().get());
            } catch (InterruptedException | ExecutionException e) {
              info.setSuccess(false);
            } catch (CancellationException ce) {
              info.setKilled(true);
            }
          }
        }
        for (BuildInfo tbr : toBeRemoved) pendingAndRunningBuilds.remove(tbr);
      } finally {
        lock.unlock();
      }
    }
  };

  void submitBuild(final BuildInfo info) throws IOException {
    LOG.debug("Submitting build " + info.toString());
    lock.lock();
    try {
      if (allTrackedBuilds.putIfAbsent(info.getLabel(), info) != null) {
        throw new IOException("Build with label " + info.getLabel() + " already exists.");
      }
      info.setQueueTime(System.currentTimeMillis());
      pendingAndRunningBuilds.put(info, executor.submit(
          () -> {
            info.setStartTime(System.currentTimeMillis());
            return dtest.startBuild(info) == 0;
          }));
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the current state
   * @return A map of build labels to build states
   */
  Map<String, BuildState> getFullState() {
    final Map<String, BuildState> buildStates = new HashMap<>(allTrackedBuilds.size());
    lock.lock();
    try {
      allTrackedBuilds.values().forEach(bi -> buildStates.put(bi.getLabel(), bi.getState()));
    } finally {
      lock.unlock();
    }
    return buildStates;
  }

  /**
   * Try to kill a build.
   * @param info build identifier
   * @return whether an attempt was made to kill the build, doesn't guarantee success.
   */
  boolean killBuild(BuildInfo info) {
    LOG.debug("Attempting to kill build " + info.toString());
    Future<Boolean> build;
    lock.lock();
    try {
      build = pendingAndRunningBuilds.get(info);
    } finally {
      lock.unlock();
    }
    if (build != null) {
      build.cancel(true);
      info.setKilled(true);
    }
    return build != null;
  }

  /**
   *
   * @param info build identifier
   * @return the logs from the build, if any
   */
  String getLogs(BuildInfo info) throws IOException {
    LOG.debug("Getting logs for build " + info.toString());
    String dir = info.getDir();
    if (dir == null) {
      // means the build hasn't run yet
      return null;
    }

    File logFile = new File(dir, DTestLogger.LOG_FILE);
    if (!logFile.exists()) return null;
    FileReader reader = new FileReader(logFile);

    char[] buffer = new char[READ_BUF_SZ];
    StringBuilder stringBuilder = new StringBuilder();
    while (reader.read(buffer) > 0) {
      stringBuilder.append(buffer);
      Arrays.fill(buffer, '\0');
    }
    return stringBuilder.toString();
  }

  /**
   * Try to clear a build.  This will remove its history from the list of finished or killed builds.
   * @param info build identifier
   */
  void clearSingleBuildHistory(BuildInfo info) throws IOException {
    lock.lock();
    try {
      allTrackedBuilds.remove(info.getLabel());
    } finally {
      lock.unlock();
    }
  }

  /**
   * Remove records of all completed and killed builds.
   */
  void clearAllHistory() {
    List<String> toForget = new ArrayList<>();
    lock.lock();
    try {
      for (BuildInfo info : allTrackedBuilds.values()) {
        if (info.isFinished()) toForget.add(info.getLabel());
      }
      allTrackedBuilds.keySet().removeAll(toForget);
    } finally {
      lock.unlock();
    }

  }

  /**
   * Find a build by the label
   * @param label build label
   * @return build info or null if not found
   */
  BuildInfo findBuild(String label) {
    lock.lock();
    try {
      return allTrackedBuilds.get(label);
    } finally {
      lock.unlock();
    }
  }

  @VisibleForTesting
  public void close() {
    LOG.info("Shutting down the test manager");
    executor.shutdownNow();
    buildCheckerPool.shutdownNow();
  }

  @VisibleForTesting
  static void resetForTesting() {
    self = null;
  }
}
