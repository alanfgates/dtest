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
package org.dtest.ozone;

import org.apache.commons.lang3.StringUtils;
import org.dtest.core.BuildInfo;
import org.dtest.core.BuildYaml;
import org.dtest.core.Config;
import org.dtest.core.ContainerClient;
import org.dtest.core.ContainerCommand;
import org.dtest.core.ContainerCommandFactory;
import org.dtest.core.ContainerResult;
import org.dtest.core.TestUtils;
import org.dtest.core.git.GitSource;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;

public class TestOzoneContainerCommandFactory {


  @Test
  public void buildCommands() throws IOException {
    TestUtils.TestLogger log = new TestUtils.TestLogger();
    Config cfg = TestUtils.buildCfg(
        BuildInfo.CFG_BUILDINFO_LABEL, "mylabel",
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildYaml.CFG_BUILDYAML_IMPL, BuildYaml.class.getName());
    OzoneContainerCommandFactory cmds = new OzoneContainerCommandFactory();
    cmds.setConfig(cfg).setLog(log);
    BuildInfo buildInfo = new BuildInfo(TestUtils.buildYaml(cfg, log, "ozonetest"), new GitSource(), true, "1");
    buildInfo.setConfig(cfg).setLog(log);
    buildInfo.getBuildDir();
    cmds.buildContainerCommands(new TestContainerClient(), buildInfo);
    log.dumpToLog();
    Assert.assertEquals(2, cmds.getCmds().size());
    Assert.assertEquals("/bin/bash -c ( cd /tmp/hadoop-hdds; /usr/bin/mvn test -Dsurefire.timeout=300 -f pom.ozone.xml)", StringUtils.join(cmds.getCmds().get(0).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/hadoop-ozone; /usr/bin/mvn test -Dsurefire.timeout=300 -f pom.ozone.xml)", StringUtils.join(cmds.getCmds().get(1).shellCommand(), " "));
  }

  private static class TestContainerClient extends ContainerClient {
    @Override
    public String getContainerBaseDir() {
      return "/tmp";
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd) {
      return null;
    }

    @Override
    public void buildImage(ContainerCommandFactory cmdFactory) {

    }

    @Override
    public void copyLogFiles(ContainerResult result, String targetDir) {

    }

    @Override
    public void removeContainer(ContainerResult result) {

    }

    @Override
    public void removeImage() {

    }
  }

}
