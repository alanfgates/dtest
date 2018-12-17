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
package org.dtest.hive;

import org.dtest.core.BuildInfo;
import org.dtest.core.BaseDockerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class HiveDockerClient extends BaseDockerClient {
  private static final Logger LOG = LoggerFactory.getLogger(HiveDockerClient.class);


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
    writer.write("    echo This build is labeled " + buildInfo.getLabel() + "; \\\n");
    writer.write("    cd " + getHomeDir() + "; \\\n");
    for (String line : buildInfo.getSrc().srcCommands(this)) writer.write(line);
    writer.write("    /usr/bin/mvn install -DskipTests; \\\n");
    writer.write("    cd itests; \\\n");
    writer.write("    /usr/bin/mvn install -DskipSparkTests -DskipTests; \\\n");
    writer.write("}\n");
    writer.close();

  }

  @Override
  public String getProjectName() {
    return "hive";
  }
}
