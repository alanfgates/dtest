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

import java.util.HashSet;
import java.util.Set;

/**
 * ContainerResult tracks the result of running a container.
 */
public class ContainerResult {
  public enum ContainerStatus { SUCCEEDED, FAILED, TIMED_OUT }

  private final ContainerCommand cmd;
  private final int rc;
  private final String logs;
  private ContainerStatus analysisResult;
  private Set<String> logFilesToFetch;

  public ContainerResult(ContainerCommand cmd, int rc, String logs) {
    this.cmd = cmd;
    this.rc = rc;
    this.logs = logs;
    logFilesToFetch = new HashSet<>();
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
   * Get any logs from the container, concatenated together as one strings
   * @return the logs
   */
  public String getLogs() {
    return logs;
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
   * @return set of logs files to fetch.
   */
  public Set<String> getLogFilesToFetch() {
    return logFilesToFetch;
  }

  /**
   * Add a file to the list of log files that should be fetched for the user.  This is a log file inside the
   * container that will be of interest to the user.  This should be called by implementations of
   * {@link ResultAnalyzer}.
   * @param logFile file to add to the list
   */
  public void addLogFileToFetch(String logFile) {
    logFilesToFetch.add(logFile);
  }

}
