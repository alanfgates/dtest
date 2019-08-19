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
package org.dtest.core.docker;

import org.dtest.core.BuildInfo;
import org.dtest.core.ContainerClient;
import org.dtest.core.ContainerCommand;
import org.dtest.core.ContainerCommandFactory;
import org.dtest.core.ContainerResult;
import org.dtest.core.impl.ProcessResults;
import org.dtest.core.impl.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of ContainerClient.  Implements container client for Docker.
 */
public class DockerContainerClient extends ContainerClient {
  /*~~
   * @document propsfile
   * @section dcc_dockerpath
   * @after resultanalyzer_impl
   * - dtest.docker.dockercontainerclient.dockerpath: Path to the docker executable.  Defaults to
   * `/usr/local/bin/docker`.
   */
  /**
   * Path to the docker executable.  Defaults to /usr/local/bin/docker
   */
  public static final String CFG_DOCKERCONTAINERCLIENT_DOCKERPATH = "dtest.docker.dockercontainerclient.dockerpath";
  private static final String CFG_DOCKERCONTAINERCLIENT_DOCKERPATH_DEFAULT = "/usr/local/bin/docker";

  protected static final Pattern IMAGE_SUCCESS = Pattern.compile("BUILD SUCCESS");
  protected static final Pattern USING_CACHE = Pattern.compile("Using cache");
  private static final String IMAGE_BASE = "dtest-";
  private static final String BUILD_CONTAINER_NAME = "image_build";

  private String imageName;
  private String dockerExec;

  @Override
  public void setBuildInfo(BuildInfo buildInfo) throws IOException {
    super.setBuildInfo(buildInfo);
    imageName = IMAGE_BASE + buildInfo.getYaml().getProjectName().toLowerCase() + "-" + buildInfo.getLabel();
  }

  @Override
  public String getContainerBaseDir() {
    return getHomeDir() + File.separator + buildInfo.getYaml().getProjectDir();
  }

  @Override
  public void buildImage(ContainerCommandFactory cmdFactory) throws IOException {
    defineImage(cmdFactory);
    log.info("Building image");
    checkBuildSucceeded(Utils.runProcess(BUILD_CONTAINER_NAME,
        cfg.getAsTime(CFG_CONTAINERCLIENT_IMAGEBUILDTIME, TimeUnit.SECONDS,
            CFG_CONTAINERCLIENT_IMAGEBUILDTIME_DEFAULT),
        log, getDockerExec(), "build", "--tag", imageName, buildInfo.getBuildDir().getAbsolutePath()));
  }

  @Override
  public ContainerResult runContainer(ContainerCommand cmd) throws IOException {
    List<String> runCmd = new ArrayList<>();
    String containerName = Utils.buildContainerName(buildInfo.getLabel(), cmd.containerSuffix());
    Collections.addAll(runCmd, getDockerExec(), "run", "--name", containerName, imageName);
    Collections.addAll(runCmd, cmd.shellCommand());
    ProcessResults res = Utils.runProcess(cmd.containerSuffix(),
        cfg.getAsTime(CFG_CONTAINERCLIENT_CONTAINERRUNTIME, TimeUnit.SECONDS,
            CFG_CONTAINERCLIENT_CONTAINERRUNTIME_DEFAULT), log, runCmd.toArray(new String[0]));
    return new ContainerResult(cmd, res.rc, res.stdout);
  }

  @Override
  public void copyLogFiles(ContainerResult result, String targetDir)
      throws IOException {
    String containerName = Utils.buildContainerName(buildInfo.getLabel(), result.getCmd().containerSuffix());
    for (String file : result.getLogFilesToFetch()) {
      ProcessResults res = Utils.runProcess("copying-files-for-" + containerName, 60, log,
          getDockerExec(), "cp", containerName + ":" + file, targetDir);
      // Don't throw if we fail to copy a file.  It's possible not every system generates every possible type of
      // output file.
    }
  }

  @Override
  public void removeContainer(ContainerResult result) throws IOException {
    String containerName = Utils.buildContainerName(buildInfo.getLabel(), result.getCmd().containerSuffix());
    if (buildInfo.shouldCleanupAfter()) {
      ProcessResults res = Utils.runProcess("cleanup", 300, log, getDockerExec(), "rm", containerName);
      if (res.rc != 0) {
        log.warn("Failed to cleanup containers: " + res.stderr);
      }
    } else {
      log.info("Skipping cleanup of container " + containerName + " since no-cleanup is set");
    }
  }

  @Override
  public void removeImage() throws IOException {
    ProcessResults res = Utils.runProcess("cleanup", 300, log, getDockerExec(), "image", "rm", imageName);
    if (res.rc != 0) {
      log.error("Failed to cleanup image: " + res.stderr);
    }
  }

  /**
   * Build the dockerfile for the image.  You can override this completely or you can call the methods
   * below that override specific parts.  The latter is recommended unless you really need to rewrite how things are
   * done.
   * @param cmdFactory used to generate the initial build command.
   * @throws IOException if the file cannot be written.
   */
  public void defineImage(ContainerCommandFactory cmdFactory) throws IOException {
    FileWriter writer = new FileWriter(new File(buildInfo.getBuildDir(), "Dockerfile"));
    writer.write("FROM " + buildInfo.getYaml().getBaseImage() + "\n");
    writer.write("\n");
    String image = buildInfo.getYaml().getBaseImage();
    if (image.startsWith("centos")) {
      writer.write("RUN yum upgrade -y && yum update -y\n");
      writer.write("RUN yum install -y ");
    } else if (image.startsWith("ubuntu") || image.startsWith("debian")) {
      writer.write("RUN apt-get update\n");
      writer.write("RUN apt-get install -y ");
    } else {
      throw new IOException("I'm sorry, I don't know how to install packages on " + image +
          ".  Currently I know how to install packages on centos, ubuntu, and debian.");
    }
    for (String pkg : buildInfo.getYaml().getRequiredPackages()) writer.write(pkg + " ");
    for (String pkg : buildInfo.getSrc().getRequiredPackages()) writer.write(pkg + " ");
    for (String pkg : cmdFactory.getRequiredPackages()) writer.write(pkg + " ");
    writer.write("\n\n");
    writer.write("RUN useradd -m " + getUser() + "\n");
    writer.write("\n");
    writer.write("USER " + getUser() + "\n");
    writer.write("\n");
    writer.write("RUN { \\\n");
    writer.write("    cd " + getHomeDir() + "; \\\n");
    for (String line : buildInfo.getSrc().srcCommands(buildInfo.getYaml())) {
      writer.write("    " + line + "; \\\n");
    }
    for (String line : cmdFactory.getInitialBuildCommand()) writer.write("    " + line + "; \\\n");
    writer.write("    echo This build is labeled " + buildInfo.getLabel() + " and was generated at " +
        LocalDateTime.now().toString() + "; \\\n");
    writer.write("}\n");
    writer.close();
  }

  /**
   * Determine whether the build succeeded.  The default implementations looks for one instance of "BUILD SUCCESS" in
   * the results.  It also checks that the return code from the run is 0.
   * @param res Result from the docker run command.
   * @throws IOException if the build did not succeed.
   */
  protected void checkBuildSucceeded(ProcessResults res) throws IOException {
    Matcher m = IMAGE_SUCCESS.matcher(res.stdout);
    // We should see "BUILD SUCCESS"
    if (res.rc != 0 || !(m.find())) {
      // We might have read some from cache, check that before bailing
      m = USING_CACHE.matcher(res.stdout);
      if (res.rc != 0 || !(m.find())) {
        // We might have read some from cache, check that before bailing
        throw new IOException("Failed to build image, see logs for error message: " + res.stderr);
      }
    }
  }

  /**
   * Get the user to run the container as.  Defaults to 'dtestuser'.
   * @return user name
   */
  protected String getUser() {
    return "dtestuser";
  }

  /**
   * Get the home directory of the user.  Defaults to /home/<i>username</i>
   * @return user home directory.
   */
  protected String getHomeDir() {
    return "/home/" + getUser();
  }

  private String getDockerExec() {
    if (dockerExec == null) {
      dockerExec = cfg.getAsString(CFG_DOCKERCONTAINERCLIENT_DOCKERPATH, CFG_DOCKERCONTAINERCLIENT_DOCKERPATH_DEFAULT);
    }
    return dockerExec;
  }
}
