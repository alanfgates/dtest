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
import org.apache.hive.testutils.dtest.DockerTest;
import org.apache.hive.testutils.dtest.impl.DTestLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class DTestManager {
  private static final Logger LOG = LoggerFactory.getLogger(DTestManager.class);
  private static final int READ_BUF_SZ = 10240;

  private final DockerTest dtest;
  private ExecutorService executor;
  private Map<BuildInfo, Boolean> finishedBuilds;
  private Map<BuildInfo, Future<Boolean>> pendingAndRunningBuilds;
  private BuildInfo currentBuild; // reset each time by checkBuilds, don't access directly
  private SortedSet<BuildInfo> pendingBuilds; // reset each time by checkBuilds, don't access directly
  private SortedSet<BuildInfo> killedBuilds; // builds tha were terminated

  public DTestManager(DockerTest dtest) {
    this.dtest = dtest;
  }

  void run() {
    LOG.info("Starting the test manager");
    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, queue);
    finishedBuilds = new HashMap<>();
    pendingAndRunningBuilds = new HashMap<>();
    killedBuilds = new TreeSet<>();
  }

  void submitBuild(BuildInfo info) {
    LOG.debug("Submitting build " + info.toString());
    pendingAndRunningBuilds.put(info, executor.submit(
        () -> {
          info.setQueueTime(System.currentTimeMillis());
          boolean r = dtest.startBuild(info) == 0;
          return r;
        }));
  }

  private synchronized void checkBuilds() {
    LOG.debug("checking builds to determine current state");
    List<BuildInfo> toBeRemoved = new ArrayList<>();
    currentBuild = null;
    pendingBuilds = new TreeSet<>(Comparator.comparingLong(BuildInfo::getQueueTime));
    for (Map.Entry<BuildInfo, Future<Boolean>> entry : pendingAndRunningBuilds.entrySet()) {
      if (entry.getValue().isDone()) {
        toBeRemoved.add(entry.getKey());
        try {
          finishedBuilds.put(entry.getKey(), entry.getValue().get());
        } catch (InterruptedException|ExecutionException e) {
          finishedBuilds.put(entry.getKey(), false);
        } catch (CancellationException ce) {
          killedBuilds.add(entry.getKey());
        }
      } else if (entry.getKey().getStartTime() > 0) {
        currentBuild = entry.getKey();
      } else {
        pendingBuilds.add(entry.getKey());
      }
    }
    for (BuildInfo tbr : toBeRemoved) pendingAndRunningBuilds.remove(tbr);
  }

  /**
   *
   * @return all of the finished builds, along with their status
   */
  Map<BuildInfo, Boolean> getFinishedBuilds() {
    checkBuilds();
    return new HashMap<>(finishedBuilds);
  }

  /**
   *
   * @return the currently running build, may be null if no build is running
   */
  BuildInfo getCurrentlyRunningBuild() {
    checkBuilds();
    return currentBuild;
  }

  /**
   *
   * @return a collection of pending builds, sorted by queue time.
   */
  Collection<BuildInfo> getPendingBuilds() {
    checkBuilds();
    return pendingBuilds;
  }

  Collection<BuildInfo> getKilledBuilds() {
    checkBuilds();
    return killedBuilds;
  }

  /**
   * Try to kill a build.
   * @param info build identifier
   * @return whether an attempt was made to kill the build, doesn't guarantee success.
   */
  boolean killBuild(BuildInfo info) {
    LOG.debug("Attempting to kill build " + info.toString());
    Future<Boolean> build = pendingAndRunningBuilds.get(info);
    if (build != null) build.cancel(true);
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
  synchronized void clearSingleBuildHistory(BuildInfo info) {
    finishedBuilds.remove(info);
    killedBuilds.remove(info);
  }

  /**
   * Remove records of all completed and killed builds.
   */
  synchronized void clearAllHistory() {
    finishedBuilds.clear();
    killedBuilds.clear();

  }

  @VisibleForTesting
  void close() {
    LOG.info("Shutting down the test manager");
    executor.shutdownNow();
  }
}
