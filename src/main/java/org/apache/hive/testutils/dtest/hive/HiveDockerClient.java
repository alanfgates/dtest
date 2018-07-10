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
package org.apache.hive.testutils.dtest.hive;

import org.apache.hive.testutils.dtest.BuildInfo;
import org.apache.hive.testutils.dtest.simple.SimpleDockerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class HiveDockerClient extends SimpleDockerClient {
  private static final Logger LOG = LoggerFactory.getLogger(HiveDockerClient.class);


  HiveDockerClient(BuildInfo info) {
    super(info);
  }

  @Override
  public void defineImage(String dir, String repo, String branch, String label) throws IOException {
    FileWriter writer = new FileWriter(dir + File.separatorChar + "Dockerfile");
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
    writer.write("    /usr/bin/git clone " + repo + "; \\\n");
    writer.write("    cd " + getProjectName() + "; \\\n");
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
  protected String getProjectName() {
    return "hive";
  }
}
