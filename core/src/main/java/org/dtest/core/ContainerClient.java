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

import org.dtest.core.docker.DockerContainerClient;
import org.dtest.core.impl.Utils;

import java.io.IOException;

/**
 * ContainerClient handles interactions with the container system.
 */
public abstract class ContainerClient extends Configurable {

  // Implementation of ContainerClient
  /**
   * Class used to implement the container.  Defaults to simple Docker.
   */
  public static final String CFG_CONTAINERCLIENT_IMPL = "dtest.core.containerclient.impl";

  /**
   * Maximum amount of time to wait for a container to run.  Defaults to 30 minutes.
   */
  public static final String CFG_CONTAINERCLIENT_CONTAINERRUNTIME = "dtest.core.containerclient.containerruntime";
  protected static final long CFG_CONTAINERCLIENT_CONTAINERRUNTIME_DEFAULT = 30 * 60;

  /**
   * Maximum amount of time to wait for an image to build.  Defaults to 3 hours.
   */
  public static final String CFG_CONTAINERCLIENT_IMAGEBUILDTIME = "dtest.core.containerclient.imagebuildtime";
  protected static final long CFG_CONTAINERCLIENT_IMAGEBUILDTIME_DEFAULT = 3 * 60 * 60;

  protected BuildInfo buildInfo;

  /**
   * Pass in the build information.  This must be called before any of the other calls.
   * @param buildInfo build information
   */
  public void setBuildInfo(BuildInfo buildInfo) {
    this.buildInfo = buildInfo;
  }

  /**
   * Return the directory in the container that commands should operate in.  Note that this refers to a directory
   * in the container, not on the build machine.
   * @return container directory.
   */
  public abstract String getContainerBaseDir();

  /**
   * Build an image.
   * @param cmdFactory factory to generate containers, needed to get the initial build info
   * @throws IOException if the image fails to build
   */
  public abstract void buildImage(ContainerCommandFactory cmdFactory) throws IOException;

  /**
   * Run a container and return the results.
   * @param cmd command to run
   * @return results from the container
   * @throws IOException if the container fails to run
   */
  public abstract ContainerResult runContainer(ContainerCommand cmd) throws IOException;

  /**
   * Print the contents failed test logs to the log.
   * @param result results from running the container
   * @param targetDir directory to copy files to
   * @throws IOException if the copy of the log files fails
   */
  public abstract void copyLogFiles(ContainerResult result, String targetDir)
      throws IOException;

  /**
   * Remove a container.
   * @param result results from running the container
   * @throws IOException if the remove fails
   */
  public abstract void removeContainer(ContainerResult result) throws IOException;

  /**
   * Remove the docker image
   * @throws IOException if the remove fails
   */
  public abstract void removeImage() throws IOException;

  static ContainerClient getInstance(Config cfg, DTestLogger log) throws IOException {
    ContainerClient cc = Utils.getInstance(cfg.getAsClass(ContainerClient.CFG_CONTAINERCLIENT_IMPL,
        ContainerClient.class, DockerContainerClient.class));
    cc.setConfig(cfg).setLog(log);
    return cc;
  }


}
