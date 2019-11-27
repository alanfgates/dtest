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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks a set of test reports that have been fetched from the container.  Some of these need to be analyzed
 * to determine if the test passed or failed.  Some may need to be kept for humans to look
 * over later.
 */
public class TestReports {
  private final File tmpDir;
  private final File reportDir;
  private final DTestLogger log;
  private Set<String> keptFiles;
  private List<String> additionalLogs;
  private Map<String, Set<File>> testToKeptFileMap;

  /**
   *
   * @param log dtest logger
   * @param containerName Name of the container the test reports are from
   * @param reportDir final directory reports will be moved to if we decide to keep them
   * @throws IOException if we are unable to create the temporary directory
   */
  public TestReports(DTestLogger log, String containerName, File reportDir) throws IOException {
    this.log = log;
    tmpDir = new File(System.getProperty("java.io.tmpdir"), "dtest-fetched-logfiles-" + containerName);
    if (!tmpDir.mkdir() && !tmpDir.isDirectory()) {
      throw new IOException("Failed to create temporary directory " + tmpDir.getAbsolutePath());
    }
    tmpDir.deleteOnExit();
    keptFiles = new HashSet<>();
    additionalLogs = new ArrayList<>();
    testToKeptFileMap = new HashMap<>();
    this.reportDir = reportDir;
  }

  /**
   * Get the directory the reports are in.
   * @return handle to the directory.
   */
  public File getDir() {
    return tmpDir;
  }

  /**
   * Move the file to a directory that will be kept after the run.
   * @param file file in this directory to move.
   * @param testName name of test to keep these logs for.
   * @throws IOException if the move fails
   */
  public void keep(File file, String testName) throws IOException {
    internalKeep(file, testName);
    keepAdditionalLogs(testName);
  }

  public void addAdditionalLog(String log) {
    additionalLogs.add(log);
  }

  /**
   * Mark the additional logs to be kept.
   * @param testName name of test to keep these logs for.
   * @throws IOException if the move fails
   */
  public void keepAdditionalLogs(String testName) throws IOException {
    for (String log : additionalLogs) {
      internalKeep(new File(tmpDir, log), testName);
    }
  }

  /**
   * Get all of the files that were kept.
   * @return collection of kept filenames.
   */
  public Map<String, Set<File>> getKeptFiles() {
    return testToKeptFileMap;
  }

  private void internalKeep(File file, String testName) throws IOException {
    if (!keptFiles.contains(file.getName()) && !file.exists()) {
      log.warn("Request to keep file " + file + ", but no such file exists");
      return;
    }
    if (keptFiles.add(file.getName())) {
      File newName = new File(reportDir, file.getName());
      if (!file.renameTo(newName)) {
        throw new IOException("Failed to move file: " + file.getAbsolutePath() + " to " + tmpDir.getAbsolutePath());
      }
    }
    Set<File> files = testToKeptFileMap.computeIfAbsent(testName, s -> new HashSet<>());
    files.add(file);
  }
}
