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

/**
 * ContainerResult tracks the result of running a container.
 */
public class ContainerResult {
  public enum ContainerStatus { SUCCEEDED, FAILED, TIMED_OUT }

  private final ContainerCommand cmd;
  private final int rc;
  private final String stdout;
  private final String containerName;
  private ContainerStatus analysisResult;
  private TestReports reports;

  /**
   *
   * @param cmd the command run in the container.
   * @param containerName name of the container;
   * @param rc result code from running the command.
   * @param stdout the output of the container as printed on stdout.
   */
  public ContainerResult(ContainerCommand cmd, String containerName, int rc, String stdout) {
    this.cmd = cmd;
    this.containerName = containerName;
    this.rc = rc;
    this.stdout = stdout;
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

  public String getContainerName() {
    return containerName;
  }

  public TestReports getReports() {
    return reports;
  }

  public void setReports(TestReports reports) {
    this.reports = reports;
  }
}
