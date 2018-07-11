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

  public ContainerCommand getCmd() {
    return cmd;
  }

  public int getRc() {
    return rc;
  }

  public String getLogs() {
    return logs;
  }

  public ContainerStatus getAnalysisResult() {
    return analysisResult;
  }

  public void setAnalysisResult(
      ContainerStatus analysisResult) {
    this.analysisResult = analysisResult;
  }

  public Set<String> getLogFilesToFetch() {
    return logFilesToFetch;
  }

  public void addLogFileToFetch(String logFile) {
    logFilesToFetch.add(logFile);
  }

}
