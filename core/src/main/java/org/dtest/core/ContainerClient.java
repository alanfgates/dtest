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
import org.dtest.core.impl.Utils;

import java.io.IOException;

public abstract class ContainerClient extends Configurable {

  @VisibleForTesting
  // Implementation of ContainerClient
  public static final String CFG_CONTAINERCLIENT_IMPL = "dtest.core.containerclient.impl";
  // Maximum amount of time to wait for container to run
  public static final String CFG_CONTAINERCLIENT_CONTAINERRUNTIME = "dtest.core.containerclient.containerruntime";
  protected static final long CFG_CONTAINERCLIENT_CONTAINERRUNTIME_DEFAULT = 30 * 60;
  // Maximum amount of time to wait for image to build
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
   * Define the container.  Usually this will mean writing a Dockerfile.
   * @throws IOException if we fail to write the docker file
   */
  public abstract void defineImage() throws IOException;

  /**
   * Return the directory in the container that commands should operate in.
   * @return container directory.
   */
  public abstract String getContainerBaseDir();

  /**
   * Build an image
   * @param dir directory to build in, must either be absolute path or relative to CWD of the
   *            process.
   * @param logger output log for tests
   * @throws IOException if the image fails to build
   */
  public abstract void buildImage(String dir, DTestLogger logger) throws IOException;

  /**
   * Run a container and return a string containing the logs
   * @param cmd command to run along with any arguments
   * @param logger output log for tests
   * @return results from the container
   * @throws IOException if the container fails to run
   */
  public abstract ContainerResult runContainer(ContainerCommand cmd, DTestLogger logger) throws IOException;

  /**
   * Print the contents failed test logs to the log.
   * @param result results from running the container
   * @param targetDir directory to copy files to
   * @param logger output log for tests
   * @throws IOException if the copy of the log files fails
   */
  public abstract void copyLogFiles(ContainerResult result, String targetDir, DTestLogger logger)
      throws IOException;

  /**
   * Remove a container.
   * @param result results from running the container
   * @param logger output log for tests
   * @throws IOException if the remove fails
   */
  public abstract void removeContainer(ContainerResult result, DTestLogger logger) throws IOException;

  /**
   * Remove the docker image
   * @param logger output log for tests
   * @throws IOException if the remove fails
   */
  public abstract void removeImage(DTestLogger logger) throws IOException;

  /**
   * Get the name of the project.
   * @return name of the project.
   */
  public abstract String getProjectName();

  static ContainerClient getInstance(Config cfg) throws IOException {
    ContainerClient cc = Utils.getInstance(cfg.getAsClass(ContainerClient.CFG_CONTAINERCLIENT_IMPL,
        ContainerClient.class, BaseDockerClient.class));
    cc.setConfig(cfg);
    return cc;
  }


}
