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
package org.apache.hive.testutils.dtest;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DockerClient implements ContainerClient {
  private static final Logger LOG = LoggerFactory.getLogger(DockerClient.class);
  private static final String CONTAINER_BASE = "hive-dtest-";
  private static final String IMAGE_BASE = "hive-dtest-image-";
  private static final Pattern IMAGE_SUCCESS = Pattern.compile("BUILD SUCCESS");

  private final int runNumber;

  DockerClient(int runNumber) {
    this.runNumber = runNumber;
  }

  // TODO - This class should be refactored to pull out the Hive specific stuff.  It should just
  // run the docker commands and have the Hive specific Dockerfile and build commands passed in.
  // The test for success for the image building should be passed in as well.

  @Override
  public void buildImage(String dir, long toWait, TimeUnit unit) throws IOException {
    long seconds = TimeUnit.SECONDS.convert(toWait, unit);
    LOG.info("Building image");
    checkBuildSucceeded(Utils.runProcess(seconds, "docker", "build", "--tag", imageName(), dir));
  }

  @Override
  public ContainerResult runContainer(long toWait, TimeUnit unit, ContainerCommand cmd)
      throws IOException {
    List<String> runCmd = new ArrayList<>();
    String containerName = createContainerName(cmd.containerName());
    runCmd.addAll(Arrays.asList("docker", "run", "--name", containerName, imageName()));
    runCmd.addAll(Arrays.asList(cmd.shellCommand()));
    long seconds = TimeUnit.SECONDS.convert(toWait, unit);
    ProcessResults res = Utils.runProcess(seconds, runCmd.toArray(new String[runCmd.size()]));
    return new ContainerResult(containerName, res.rc, res.stdout);
  }

  @VisibleForTesting
  static void checkBuildSucceeded(ProcessResults res) throws IOException {
    Matcher m = IMAGE_SUCCESS.matcher(res.stdout);
    // We should see "BUILD SUCCESS" twice, once for the main build and once for itests
    if (res.rc != 0 || !(m.find() && m.find())) {
      throw new IOException("Failed to build image, see logs for error message: " + res.stderr);
    }
  }

  private String imageName() {
    return IMAGE_BASE + runNumber;
  }

  private String createContainerName(String name) {
    return CONTAINER_BASE + runNumber + "_" + name;
  }

}
