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
package org.apache.hive.testutils.dtest.impl;

import com.google.common.annotations.VisibleForTesting;
import org.apache.hive.testutils.dtest.ContainerClient;
import org.apache.hive.testutils.dtest.ContainerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@VisibleForTesting
public class DockerClient implements ContainerClient {
  private static final Logger LOG = LoggerFactory.getLogger(DockerClient.class);
  private static final String IMAGE_BASE = "dtest-image-";
  private static final Pattern IMAGE_SUCCESS = Pattern.compile("BUILD SUCCESS");
  private static final Pattern USING_CACHE = Pattern.compile("Using cache");
  private static final String BUILD_CONTAINER_NAME = "image_build";
  static final String USER = "dtestuser";
  private static final String HOME_DIR = File.separator + "home" + File.separator + USER;

  private final String label;
  private final String imageName;

  DockerClient(String label) {
    this.label = label;
    imageName = IMAGE_BASE + label;
  }

  @Override
  public String getContainerBaseDir() {
    return HOME_DIR + File.separator + "hive";
  }

  @Override
  public void defineImage(String dir, String repo, String branch, String label) throws IOException {
    if (label == null) label = UUID.randomUUID().toString();
    FileWriter writer = new FileWriter(dir + File.separatorChar + "Dockerfile");
    writer.write("FROM centos\n");
    writer.write("\n");
    writer.write("RUN yum upgrade -y && \\\n");
    writer.write("    yum update -y && \\\n");
    writer.write("    yum install -y java-1.8.0-openjdk-devel unzip git maven\n");
    writer.write("\n");
    writer.write("RUN useradd -m " + USER + "\n");
    writer.write("\n");
    writer.write("USER " + USER + "\n");
    writer.write("\n");
    writer.write("RUN { \\\n");
    writer.write("    cd " + HOME_DIR + "; \\\n");
    writer.write("    /usr/bin/git clone " + repo + "; \\\n");
    writer.write("    cd hive; \\\n");
    writer.write("    /usr/bin/git checkout " + branch + "; \\\n");
    writer.write("    /usr/bin/mvn install -Dtest=TestMetastoreConf; \\\n"); // Need a quick test
    // that actually runs so it downloads the surefire jar from maven
    writer.write("    cd itests; \\\n");
    writer.write("    /usr/bin/mvn install -DskipSparkTests -DskipTests; \\\n");
    writer.write("    echo This build is labeled " + label + "; \\\n");
    writer.write("}\n");
    writer.close();

  }

  @Override
  public void buildImage(String dir, long toWait, DTestLogger logger)
      throws IOException {
    LOG.info("Building image");
    checkBuildSucceeded(Utils.runProcess(BUILD_CONTAINER_NAME, toWait, logger, "docker", "build",
        "--tag", imageName, dir));
  }

  @Override
  public ContainerResult runContainer(long toWait, ContainerCommand cmd,
                                      DTestLogger logger) throws IOException {
    List<String> runCmd = new ArrayList<>();
    String containerName = Utils.buildContainerName(label, cmd.containerSuffix());
    Collections.addAll(runCmd, "docker", "run", "--name", containerName, imageName);
    Collections.addAll(runCmd, cmd.shellCommand());
    ProcessResults res = Utils.runProcess(cmd.containerSuffix(), toWait, logger,
        runCmd.toArray(new String[runCmd.size()]));
    return new ContainerResult(cmd, res.rc, res.stdout);
  }

  @Override
  public void copyLogFiles(ContainerResult result, String targetDir, DTestLogger logger)
      throws IOException {
    String containerName = Utils.buildContainerName(label, result.getCmd().containerSuffix());
    for (String file : result.getLogFilesToFetch()) {
      ProcessResults res = Utils.runProcess("copying-files-for-" + containerName, 60, logger,
          "docker", "cp", containerName + ":" + file, targetDir);
      if (res.rc != 0) throw new IOException("Failed to copy logfile " + res.stderr);
    }
  }

  @VisibleForTesting
  public static void checkBuildSucceeded(ProcessResults res) throws IOException {
    Matcher m = IMAGE_SUCCESS.matcher(res.stdout);
    // We should see "BUILD SUCCESS" twice, once for the main build and once for itests
    if (res.rc != 0 || !(m.find() && m.find())) {
      // We might have read some from cache, check that before bailing
      m = USING_CACHE.matcher(res.stdout);
      if (res.rc != 0 || !(m.find() && m.find())) {
        // We might have read some from cache, check that before bailing
        throw new IOException("Failed to build image, see logs for error message: " + res.stderr);
      }
    }
  }

  @Override
  public void removeContainer(ContainerResult result, DTestLogger logger) throws IOException {
    String containerName = Utils.buildContainerName(label, result.getCmd().containerSuffix());
    ProcessResults res = Utils.runProcess("cleanup", 300, logger, "docker", "rm", containerName);
    if (res.rc != 0) {
      LOG.warn("Failed to cleanup containers: " + res.stderr);
    }
  }

  @Override
  public void removeImage(DTestLogger logger) throws IOException {
    ProcessResults res = Utils.runProcess("cleanup", 300, logger, "docker", "image", "rm", imageName);
    if (res.rc != 0) {
      LOG.warn("Failed to cleanup containers: " + res.stderr);
    }

  }
}
