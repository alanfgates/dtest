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

import com.google.common.annotations.VisibleForTesting;
import org.dtest.core.impl.ProcessResults;
import org.dtest.core.impl.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseDockerClient extends ContainerClient {
  private static final Logger LOG = LoggerFactory.getLogger(BaseDockerClient.class);
  private static final String IMAGE_BASE = "dtest-image-";
  private static final Pattern IMAGE_SUCCESS = Pattern.compile("BUILD SUCCESS");
  private static final Pattern USING_CACHE = Pattern.compile("Using cache");
  private static final String BUILD_CONTAINER_NAME = "image_build";

  private String imageName;

  @Override
  public void setBuildInfo(BuildInfo buildInfo) {
    super.setBuildInfo(buildInfo);
    imageName = IMAGE_BASE + buildInfo.getLabel();
  }

  @Override
  public String getContainerBaseDir() {
    return getHomeDir() + File.separator + getProjectName();
  }

  @Override
  public void defineImage() throws IOException {
    FileWriter writer = new FileWriter(buildInfo.getDir() + File.separatorChar + "Dockerfile");
    writer.write("FROM centos\n");
    writer.write("\n");
    writer.write("RUN yum upgrade -y && \\\n");
    writer.write("    yum update -y && \\\n");
    writer.write("    yum install -y java-1.8.0-openjdk-devel unzip git maven\n");
    writer.write("\n");
    writer.write("RUN useradd -m " + getUser() + "\n");
    writer.write("\n");
    writer.write("USER " + getUser() + "\n");
    writer.write("\n");
    writer.write("RUN { \\\n");
    writer.write("    cd " + getHomeDir() + "; \\\n");
    for (String line : buildInfo.getSrc().srcCommands(this)) writer.write(line);
    writer.write("    /usr/bin/mvn install -DskipTests; \\\n"); // Need a quick test
    writer.write("    echo This build is labeled " + buildInfo.getLabel() + "; \\\n");
    writer.write("}\n");
    writer.close();

  }

  @Override
  public void buildImage(String dir, DTestLogger logger)
      throws IOException {
    LOG.info("Building image");
    checkBuildSucceeded(Utils.runProcess(BUILD_CONTAINER_NAME,
        getConfig().getAsTime(CFG_CONTAINERCLIENT_IMAGEBUILDTIME, TimeUnit.SECONDS,
            CFG_CONTAINERCLIENT_IMAGEBUILDTIME_DEFAULT),
        logger, "docker", "build", "--tag", imageName, dir));
  }

  @Override
  public ContainerResult runContainer(ContainerCommand cmd,
                                      DTestLogger logger) throws IOException {
    List<String> runCmd = new ArrayList<>();
    String containerName = Utils.buildContainerName(buildInfo.getLabel(), cmd.containerSuffix());
    Collections.addAll(runCmd, "docker", "run", "--name", containerName, imageName);
    Collections.addAll(runCmd, cmd.shellCommand());
    ProcessResults res = Utils.runProcess(cmd.containerSuffix(),
        getConfig().getAsTime(CFG_CONTAINERCLIENT_CONTAINERRUNTIME, TimeUnit.SECONDS,
            CFG_CONTAINERCLIENT_CONTAINERRUNTIME_DEFAULT), logger, runCmd.toArray(new String[runCmd.size()]));
    return new ContainerResult(cmd, res.rc, res.stdout);
  }

  @Override
  public void copyLogFiles(ContainerResult result, String targetDir, DTestLogger logger)
      throws IOException {
    String containerName = Utils.buildContainerName(buildInfo.getLabel(), result.getCmd().containerSuffix());
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
    String containerName = Utils.buildContainerName(buildInfo.getLabel(), result.getCmd().containerSuffix());
    if (buildInfo.shouldCleanupAfter()) {
      ProcessResults res = Utils.runProcess("cleanup", 300, logger, "docker", "rm", containerName);
      if (res.rc != 0) {
        LOG.warn("Failed to cleanup containers: " + res.stderr);
      }
    } else {
      LOG.info("Skipping cleanup of container " + containerName + " since no-cleanup is set");
    }
  }

  @Override
  public void removeImage(DTestLogger logger) throws IOException {
    if (buildInfo.shouldCleanupAfter()) {
      ProcessResults res =
          Utils.runProcess("cleanup", 300, logger, "docker", "image", "rm", imageName);
      if (res.rc != 0) {
        LOG.warn("Failed to cleanup containers: " + res.stderr);
      }
    } else {
      LOG.info("Skipping cleanup of image " + imageName + " since no-cleanup is set");
    }
  }

  @Override
  public String getProjectName() {
    return "dtest";
  }
  protected String getUser() {
    return "dtestuser";
  }

  protected String getHomeDir() {
    return "/home/" + getUser();
  }

}
