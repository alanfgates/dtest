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

import org.apache.hive.testutils.dtest.impl.ContainerResult;
import org.apache.hive.testutils.dtest.impl.DTestLogger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface ContainerClient {

  /**
   * Define the container.  Usually this will mean writing a Dockerfile.
   * @param dir Directory for the build.
   * @param repo git repository to pull from
   * @param branch git branch to use
   * @param label identifying value for this build
   * @throws IOException if we fail to write the docker file
   */
  void defineImage(String dir, String repo, String branch, String label) throws IOException;

  /**
   * Return the directory in the container that commands should operate in.
   * @return container directory.
   */
  String getContainerBaseDir();

  /**
   * Build an image
   * @param dir directory to build in, must either be absolute path or relative to CWD of the
   *            process.
   * @param toWait how long to wait for this command
   * @param unit toWait is measured in
   * @param logger output log for tests
   * @throws IOException if the image fails to build
   */
  void buildImage(String dir, long toWait, TimeUnit unit, DTestLogger logger) throws IOException;

  /**
   * Run a container and return a string containing the logs
   * @param toWait how long to wait for this run
   * @param unit toWait is measured in
   * @param cmd command to run along with any arguments
   * @param logger output log for tests
   * @return results from the container
   * @throws IOException if the container fails to run
   */
  ContainerResult runContainer(long toWait, TimeUnit unit, ContainerCommand cmd, DTestLogger logger)
      throws IOException;

  /**
   * Print the contents failed test logs to the log.
   * @param result results from running the container
   * @param targetDir directory to copy files to
   * @param logger output log for tests
   * @throws IOException if the copy of the log files fails
   */
  default void copyLogFiles(ContainerResult result, String targetDir, DTestLogger logger)
      throws IOException {

  }
}
