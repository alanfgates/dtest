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
import org.dtest.core.CodeSource;
import org.dtest.core.Config;
import org.dtest.core.ContainerCommandFactory;
import org.dtest.core.TestUtils;
import org.dtest.core.git.GitSource;
import org.dtest.core.mvn.MavenContainerCommandFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TestDockerContainerClient {

  @Test
  public void testDefineImage() throws IOException {
    DockerContainerClient client = new DockerContainerClient();
    Config cfg = TestUtils.buildCfg(
        BuildInfo.CFG_BUILDINFO_LABEL, "needsomething",
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        GitSource.CFG_CODESOURCE_REPO, "repo",
        GitSource.CFG_CODESOURCE_BRANCH, "branch");
    client.setConfig(cfg);
    CodeSource src = new GitSource();
    src.setConfig(cfg);
    BuildInfo info = new BuildInfo(System.getProperty("java.io.tmpdir"), src, true);
    info.setConfig(cfg);
    client.setBuildInfo(info);
    ContainerCommandFactory cmdFactory = new MavenContainerCommandFactory();
    cmdFactory.setConfig(cfg);
    client.defineImage(cmdFactory);

    BufferedReader reader = new BufferedReader(new FileReader(info.getBuildDir() + File.separatorChar + "Dockerfile"));
    StringBuilder buf = new StringBuilder();
    String line;
    do {
      line = reader.readLine();
      if (line != null) buf.append(line).append("\n");
    } while (line != null);
    reader.close();

    Assert.assertEquals("FROM centos\n" +
        "\n" +
        "RUN yum upgrade -y && yum update -y && yum install -y java-1.8.0-openjdk-devel git unzip maven \n" +
        "\n" +
        "RUN useradd -m dtestuser\n" +
        "\n" +
        "USER dtestuser\n" +
        "\n" +
        "RUN { \\\n" +
        "    cd /home/dtestuser; \\\n" +
        "    /usr/bin/git clone repo;     cd dtest;     /usr/bin/git checkout branch; \\\n" +
        "/usr/bin/mvn install -DskipTests; \\\n" +
        "    echo This build is labeled needsomething; \\\n" +
        "}\n", buf.toString());




  }
}
