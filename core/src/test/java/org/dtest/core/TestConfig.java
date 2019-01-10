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

import org.dtest.core.git.GitSource;
import org.dtest.core.impl.Utils;
import org.dtest.core.mvn.MavenContainerCommand;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class TestConfig {

  private static class MySource extends CodeSource {
    @Override
    public List<String> srcCommands(BuildYaml yaml) throws IOException {
      return null;
    }

    @Override
    public String getDefaultBranch() {
      return null;
    }

    @Override
    public List<String> getRequiredPackages() {
      return null;
    }
  }

  @Test
  public void testConfigFile() throws IOException {
    File propertiesFile = new File(System.getProperty("java.io.tmpdir"), Config.PROPERTIES_FILE);
    propertiesFile.deleteOnExit();
    FileWriter writer = new FileWriter(propertiesFile);
    writer.write(BuildInfo.CFG_BUILDINFO_BASEDIR + " = /base/dir\n");
    writer.write(ContainerClient.CFG_CONTAINERCLIENT_IMAGEBUILDTIME + " = 1min\n");
    writer.write(CodeSource.CFG_CODESOURCE_IMPL + " = " + MySource.class.getName() + "\n");
    writer.close();

    // Make sure we don't overwrite existing properites
    Properties props = new Properties();
    props.setProperty(ContainerClient.CFG_CONTAINERCLIENT_IMAGEBUILDTIME, "1h");
    Config cfg = new Config(System.getProperty("java.io.tmpdir"), props);

    // Test ones from the file
    Assert.assertEquals("/base/dir", cfg.getAsString(BuildInfo.CFG_BUILDINFO_BASEDIR));
    Assert.assertEquals(3600L, cfg.getAsTime(ContainerClient.CFG_CONTAINERCLIENT_IMAGEBUILDTIME, TimeUnit.SECONDS));
    // Test default values are set
    Assert.assertEquals(10, cfg.getAsInt(ContainerCommandFactory.CFG_CONTAINERCOMMANDFACTORY_TESTSPERCONTAINER,
        ContainerCommandFactory.CFG_CONTAINERCOMMANDFACTORY_TESTSPERCONTAINER_DEFAULT));
    Assert.assertEquals(MySource.class, cfg.getAsClass(CodeSource.CFG_CODESOURCE_IMPL, CodeSource.class, GitSource.class));
    // Test that later changes to the properties don't affect the config object
    props.setProperty("x.y.z", "5");
    Assert.assertEquals(0, cfg.getAsInt("x.y.z"));

    // Test values with no key match don't go sideways
    Assert.assertEquals(0, cfg.getAsInt("no.such"));
    Assert.assertEquals(0L, cfg.getAsTime("no.such", TimeUnit.SECONDS));
    Assert.assertNull(cfg.getAsString("no.such"));

  }

  @Test(expected = IOException.class)
  public void testBadClass() throws IOException {
    Properties props = new Properties();
    props.setProperty("test.bad.class", "NoSuchClass");
    Config cfg = new Config(props);
    Utils.getInstance(cfg.getAsClass("test.bad.class", ContainerCommand.class, MavenContainerCommand.class));
  }

  @Test
  public void testFromSystemProperties() {
    System.setProperty("dtest.testconfig.testval", "5");
    Config cfg = new Config();
    Assert.assertEquals(5, cfg.getAsInt("dtest.testconfig.testval"));
  }
}
