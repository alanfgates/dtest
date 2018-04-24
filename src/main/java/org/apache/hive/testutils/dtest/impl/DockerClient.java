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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@VisibleForTesting
public class DockerClient implements ContainerClient {
  private static final Logger LOG = LoggerFactory.getLogger(DockerClient.class);
  private static final String IMAGE_BASE = "dtest-image-";
  private static final Pattern IMAGE_SUCCESS = Pattern.compile("BUILD SUCCESS");
  private static final Pattern USING_CACHE = Pattern.compile("Using cache");
  private static final String BUILD_CONTAINER_NAME = "image_build";

  private final String label;

  DockerClient(String label) {
    this.label = label;
  }

  @Override
  public String getContainerBaseDir() {
    return "/root/hive";
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
    writer.write("RUN { \\\n");
    writer.write("    cd /root; \\\n");
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
  public void buildImage(String dir, long toWait, TimeUnit unit, DTestLogger logger)
      throws IOException {
    long seconds = TimeUnit.SECONDS.convert(toWait, unit);
    LOG.info("Building image");
    checkBuildSucceeded(Utils.runProcess(BUILD_CONTAINER_NAME, seconds, logger, "docker", "build",
        "--tag", imageName(), dir));
  }

  @Override
  public ContainerResult runContainer(long toWait, TimeUnit unit, ContainerCommand cmd,
                                      DTestLogger logger) throws IOException {
    List<String> runCmd = new ArrayList<>();
    String containerName = Utils.buildContainerName(label, cmd.containerName());
    runCmd.addAll(Arrays.asList("docker", "run", "--name", containerName, imageName()));
    runCmd.addAll(Arrays.asList(cmd.shellCommand()));
    long seconds = TimeUnit.SECONDS.convert(toWait, unit);
    ProcessResults res = Utils.runProcess(cmd.containerName(), seconds, logger,
        runCmd.toArray(new String[runCmd.size()]));
    return new ContainerResult(containerName, res.rc, res.stdout);
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

  private String imageName() {
    return IMAGE_BASE + label;
  }

}
