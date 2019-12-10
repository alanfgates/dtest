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
package org.dtest.core.testutils;

import org.dtest.core.ContainerClient;
import org.dtest.core.ContainerCommand;
import org.dtest.core.ContainerCommandFactory;
import org.dtest.core.ContainerResult;
import org.dtest.core.Reporter;
import org.dtest.core.ResultAnalyzer;
import org.dtest.core.TestReports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This ContainerClient won't actually run anything.  You must provide it with the result and files you wish it
 * to return.
 */
public class MockContainerClient extends ContainerClient {
  protected final String containerName;
  private final String stdout;
  private final int rc;
  private final Map<String, String> testReports;
  private final File baseDir;

  /**
   *
   * @param containerName name of the container, doesn't really matter
   * @param cannedDir canned directory to get logs from, should exist in core/src/test/resources/logs
   * @param buildDir build directory for this test
   * @param rc return code from the container
   * @throws IOException if files can't properly be copied around
   */
  public MockContainerClient(String containerName, String cannedDir, File buildDir, int rc) throws IOException {
    baseDir = buildDir;
    this.containerName = containerName;
    this.rc = rc;
    testReports = new HashMap<>();
    if (cannedDir != null) {
      File logDir = new File(System.getProperty("dtest.testonly.conf.dir") + File.separator + "logs" + File.separator + cannedDir);
      assert logDir.isDirectory() : "Expected directory " + logDir.getAbsolutePath() + " to exist.";
      this.stdout = TestUtils.readLogFile(new File(logDir, "stdout").getAbsolutePath());
      File[] files = logDir.listFiles();
      assert files != null : "Expected some files";
      for (File file : files) {
        testReports.put(file.getName(), TestUtils.readLogFile(file.getAbsolutePath()));
      }
    } else {
      stdout = "";
    }

  }

  @Override
  public String getContainerBaseDir() {
    return baseDir.getAbsolutePath();
  }

  @Override
  public void buildImage(ContainerCommandFactory cmdFactory) throws IOException {

  }

  @Override
  public ContainerResult runContainer(ContainerCommand cmd) {
    return new ContainerResult(cmd, containerName, rc, stdout);
  }

  @Override
  public void fetchTestReports(ContainerResult result, ResultAnalyzer analyzer, Reporter reporter, String[] additionalLogs) throws IOException {
    result.setReports(new TestReports(log, result.getContainerName(), reporter.getLogDirForContainer(result)));
    for (Map.Entry<String, String> e : testReports.entrySet()) {
      FileWriter writer = new FileWriter(new File(result.getReports().getTempDir(), e.getKey()));
      writer.write(e.getValue());
      writer.close();
    }
    if (additionalLogs != null) {
      for (String additionalLog : additionalLogs) {
        result.getReports().addAdditionalLog(additionalLog);
      }
    }
  }

  @Override
  public void removeContainer(ContainerResult result) {

  }

  @Override
  public void removeImage() {

  }
}
