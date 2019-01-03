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
import org.dtest.core.BuildYaml;
import org.dtest.core.CodeSource;
import org.dtest.core.Config;
import org.dtest.core.ContainerCommandFactory;
import org.dtest.core.TestUtils;
import org.dtest.core.docker.DockerContainerClient;
import org.dtest.core.git.GitSource;
import org.dtest.core.impl.ProcessResults;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TestHiveDockerClient {
  private Config cfg;
  private TestUtils.TestLogger log;

  @Before
  public void buildConfigAndLog() {
    cfg = TestUtils.buildCfg(
        BuildInfo.CFG_BUILDINFO_LABEL, "needsomething",
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        GitSource.CFG_CODESOURCE_REPO, "repo",
        GitSource.CFG_CODESOURCE_BRANCH, "branch",
        BuildYaml.CFG_BUILDYAML_IMPL, HiveBuildYaml.class.getName());
    log = new TestUtils.TestLogger();
  }

  @After
  public void dumpLog() {
    log.dumpToLog();
  }

  @Test
  public void testDefineImage() throws IOException {
    DockerContainerClient client = new HiveDockerClient();
    client.setConfig(cfg).setLog(log);
    CodeSource src = new GitSource();
    src.setConfig(cfg).setLog(log);
    BuildInfo info = new BuildInfo(TestUtils.getConfDir(), src, true);
    info.setConfig(cfg).setLog(log);
    client.setBuildInfo(info);
    ContainerCommandFactory cmdFactory = new HiveContainerCommandFactory();
    cmdFactory.setConfig(cfg).setLog(log);
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
        "RUN yum upgrade -y && yum update -y\n" +
        "RUN yum install -y java-1.8.0-openjdk-devel git unzip maven \n" +
        "\n" +
        "RUN useradd -m dtestuser\n" +
        "\n" +
        "USER dtestuser\n" +
        "\n" +
        "RUN { \\\n" +
        "    cd /home/dtestuser; \\\n" +
        "    /usr/bin/git clone repo; \\\n" +
        "    cd hive; \\\n" +
        "    /usr/bin/git checkout branch; \\\n" +
        "    /usr/bin/mvn install -DskipTests; \\\n" +
        "    cd itests; \\\n" +
        "    /usr/bin/mvn install -DskipSparkTests -DskipTests; \\\n" +
        "    echo This build is labeled needsomething; \\\n" +
        "}\n", buf.toString());
  }

  @Test
  public void successfulImageBuild() throws IOException {
    HiveDockerClient client = new HiveDockerClient();
    client.setConfig(cfg).setLog(log);
    client.checkBuildSucceeded(new ProcessResults(
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Packaging .................................... SUCCESS [1.924s]\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD SUCCESS\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 9:45.700s\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 18:19:45 UTC 2018\n" +
            "2018-04-04T11:19:46,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 514M/11950M\n" +
            "2018-04-04T11:19:46,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - QFile Accumulo Tests ........... SUCCESS [6.899s]\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] JMH benchmark: Hive ............................... SUCCESS [21.935s]\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests - Hadoop 2 .......... SUCCESS [3.726s]\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests with miniKdc ........ SUCCESS [4.960s]\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD SUCCESS\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 2:07.203s\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 18:21:54 UTC 2018\n" +
            "2018-04-04T11:21:55,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 493M/4670M\n" +
            "2018-04-04T11:21:55,982  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n", "", 0));
  }

  @Test(expected = IOException.class)
  public void imageBuildSucceededButBuildFailed() throws IOException {
    HiveDockerClient client = new HiveDockerClient();
    client.setConfig(cfg).setLog(log);
    client.checkBuildSucceeded(new ProcessResults(
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Packaging .................................... SUCCESS [1.924s]\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD SUCCESS\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 9:45.700s\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 18:19:45 UTC 2018\n" +
            "2018-04-04T11:19:46,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 514M/11950M\n" +
            "2018-04-04T11:19:46,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Custom UDFs - udf-classloader-udf2  SUCCESS [0.780s]\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Custom UDFs - udf-vectorized-badexample  SUCCESS [0.896s]\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - HCatalog Unit Tests ............ FAILURE [1:44.582s]\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - QFile Druid Tests .............. SKIPPED\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Testing Utilities .............. SKIPPED\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests ..................... SKIPPED\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Blobstore Tests ................ SKIPPED\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Test Serde ..................... SKIPPED\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - QFile Tests .................... SKIPPED\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - QFile Accumulo Tests ........... SKIPPED\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] JMH benchmark: Hive ............................... SKIPPED\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests - Hadoop 2 .......... SKIPPED\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests with miniKdc ........ SKIPPED\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD FAILURE\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 2:22.569s\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 20:39:58 UTC 2018\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 83M/3436M\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n", "", 0));
  }

  @Test(expected = IOException.class)
  public void imageBuildSuccessfulButBothBuildsFailed() throws IOException {
    HiveDockerClient client = new HiveDockerClient();
    client.setConfig(cfg).setLog(log);
    client.checkBuildSucceeded(new ProcessResults(
        "2018-04-04T13:37:34,371  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive TestUtils .................................... SKIPPED\n" +
            "2018-04-04T13:37:34,371  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Packaging .................................... SKIPPED\n" +
            "2018-04-04T13:37:34,371  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T13:37:34,371  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD FAILURE\n" +
            "2018-04-04T13:37:34,371  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T13:37:34,371  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 2:47.572s\n" +
            "2018-04-04T13:37:34,371  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 20:37:33 UTC 2018\n" +
            "2018-04-04T13:37:35,371  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 138M/2701M\n" +
            "2018-04-04T13:37:35,371  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests - Hadoop 2 .......... SKIPPED\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests with miniKdc ........ SKIPPED\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD FAILURE\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 2:22.569s\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 20:39:58 UTC 2018\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 83M/3436M\n" +
            "2018-04-04T13:39:58,792  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n", "", 0));
  }

  @Test(expected = IOException.class)
  public void imageBuildFailed() throws IOException {
    HiveDockerClient client = new HiveDockerClient();
    client.setConfig(cfg).setLog(log);
    client.checkBuildSucceeded(new ProcessResults(
        "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Packaging .................................... SUCCESS [1.924s]\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD SUCCESS\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 9:45.700s\n" +
            "2018-04-04T11:19:45,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 18:19:45 UTC 2018\n" +
            "2018-04-04T11:19:46,741  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 514M/11950M\n" +
            "2018-04-04T11:19:46,741  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - QFile Accumulo Tests ........... SUCCESS [6.899s]\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] JMH benchmark: Hive ............................... SUCCESS [21.935s]\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests - Hadoop 2 .......... SUCCESS [3.726s]\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Hive Integration - Unit Tests with miniKdc ........ SUCCESS [4.960s]\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] BUILD SUCCESS\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Total time: 2:07.203s\n" +
            "2018-04-04T11:21:54,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Finished at: Wed Apr 04 18:21:54 UTC 2018\n" +
            "2018-04-04T11:21:55,981  INFO [Thread-1] dtest.StreamPumper: [INFO] Final Memory: 493M/4670M\n" +
            "2018-04-04T11:21:55,982  INFO [Thread-1] dtest.StreamPumper: [INFO] ------------------------------------------------------------------------\n", "", 1));
  }
}
