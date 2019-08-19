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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * ContainerResult tracks the result of running a container.
 */
public class ContainerResult {
  public enum ContainerStatus { SUCCEEDED, FAILED, TIMED_OUT }

  private final ContainerCommand cmd;
  private final int rc;
  private final String stdout;
  private ContainerStatus analysisResult;
  private Map<String, List<String>> logFilesToFetch;

  /**
   *
   * @param cmd the command run in the container.
   * @param rc result code from running the command.
   * @param stdout the output of the container as printed on stdout.
   */
  public ContainerResult(ContainerCommand cmd, int rc, String stdout) {
    this.cmd = cmd;
    this.rc = rc;
    this.stdout = stdout;
    logFilesToFetch = new HashMap<>();
  }

  /**
   * Get the command the container ran.
   * @return command
   */
  public ContainerCommand getCmd() {
    return cmd;
  }

  /**
   * Get the return code from the command that ran the container
   * @return rc
   */
  public int getRc() {
    return rc;
  }

  /**
   * Get the output from the container, concatenated together as one string
   * @return what the container sent to stdout.
   */
  public String getStdout() {
    return stdout;
  }

  /**
   * Get the status of the container.  This class does not analyze the execution of the container, that is
   * handled by {@link ResultAnalyzer}.
   * @return status
   */
  public ContainerStatus getAnalysisResult() {
    return analysisResult;
  }

  /**
   * Set the result of the container.
   * @param analysisResult status
   */
  public void setAnalysisResult(ContainerStatus analysisResult) {
    this.analysisResult = analysisResult;
  }

  /**
   * Get a list of files from the container that should be fetched for the user.  Examples include log4j logs,
   * build logs, etc. that the user may want to see to analyze the build.  DockerTest will fetch these and
   * tar them up for the caller.
   * @return set of logs files to fetch, keyed to the test name
   */
  public Map<String, List<String>> getLogFilesToFetch() {
    return logFilesToFetch;
  }

  /**
   * Add a file to the list of log files that should be fetched for the user.  This is a log file inside the
   * container that will be of interest to the user.  This should be called by implementations of
   * {@link ResultAnalyzer}.
   * @param testName name of the test this log file is for
   * @param logFile file to add to the list
   */
  public void addLogFileToFetch(String testName, String logFile) {
    List<String> logFiles = logFilesToFetch.computeIfAbsent(testName, s -> new ArrayList<>());
    logFiles.add(logFile);
  }

}
