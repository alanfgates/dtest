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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility class to build various pieces we need like the Docker file and the commands
 */
class DockerBuilder {

  /**
   * Build the docker file
   * @param dir Directory the docker file is in
   * @param repo git repository to pull from
   * @param branch git branch to use
   * @param buildNum build number
   * @throws IOException if we fail to write the docker file
   */
  static void createDockerFile(String dir, String repo, String branch, int buildNum)
      throws IOException {
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
    writer.write("    /usr/bin/mvn install -Dtest=nosuch; \\\n");
    writer.write("    cd itests; \\\n");
    writer.write("    /usr/bin/mvn install -Dtest=nosuch -DskipSparkTests; \\\n");
    writer.write("    echo This is build number " + buildNum + "; \\\n");
    writer.write("}\n");
    writer.close();
  }
}
