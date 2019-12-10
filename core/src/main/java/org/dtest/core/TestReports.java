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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
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
    keptFiles = new HashSet<>();
    additionalLogs = new ArrayList<>();
    testToKeptFileMap = new HashMap<>();
    this.reportDir = reportDir;
  }

  /**
   * Get the temporary directory the reports are cached in after being moved from the docker container.
   * @return handle to the directory.
   */
  public File getTempDir() {
    return tmpDir;
  }

  /**
   * Get the directory that files are moved to for the {@link Reporter}.
   * @return handle to the directory
   */
  public File getReportDir() {
    return reportDir;
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

  /**
   * Add a file to the list of additional logs to keep.  The implementation assumes that this log has been
   * fetched from the container an placed in {@link #tmpDir} by the {@link ContainerClient}.
   * @param log Filename
   */
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

  /**
   * Removes the temporary directory and all the files that were placed in it.
   */
  public void cleanupTempDir() {
    try {
      Files.walkFileTree(tmpDir.toPath(), new FileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
          log.warn("Unable to delete file " + file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      log.warn("Caught exception while trying to clean up temporary directory " + tmpDir, e);
    }
  }

  private void internalKeep(File file, String testName) throws IOException {
    if (!keptFiles.contains(file.getName()) && !file.exists()) {
      log.warn("Request to keep file " + file + ", but no such file exists");
      return;
    }
    if (keptFiles.add(file.getName())) {
      createReportDirIfNotExists();
      File newName = new File(reportDir, file.getName());
      if (!file.renameTo(newName)) {
        throw new IOException("Failed to move file: " + file.getAbsolutePath() + " to " + tmpDir.getAbsolutePath());
      }
    }
    Set<File> files = testToKeptFileMap.computeIfAbsent(testName, s -> new HashSet<>());
    files.add(file);
  }

  private void createReportDirIfNotExists() {
    if (!reportDir.exists()) {
      log.info("Creating directory " + reportDir.getAbsolutePath() + " for reports");
      if (!reportDir.mkdir()) {
        log.warn("Expected to create directory " + reportDir + ", but it appears to already exist");
      }
    }
  }
}
