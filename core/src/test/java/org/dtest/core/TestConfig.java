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
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TestConfig {

  @Test
  public void testConfigFile() throws IOException {
    File propertiesFile = new File(System.getProperty("java.io.tmpdir"), Config.PROPERTIES_FILE);
    propertiesFile.deleteOnExit();
    FileWriter writer = new FileWriter(propertiesFile);
    writer.write(GitSource.CFG_GIT_BRANCH + " = branch\n");
    writer.write(ContainerClient.CFG_IMAGE_BUILD_TIME + " = 1min");
    writer.close();

    // Make sure we don't overwrite existing properites
    Config.set(ContainerClient.CFG_IMAGE_BUILD_TIME, "1h");
    Config.fromConfigFile(System.getProperty("java.io.tmpdir"));

    DockerTest t = new DockerTest(null, null);

    // Test ones from the file
    Assert.assertEquals("branch", Config.getAsString(GitSource.CFG_GIT_BRANCH));
    Assert.assertEquals(3600L, Config.getAsTime(ContainerClient.CFG_IMAGE_BUILD_TIME, TimeUnit.SECONDS));
    // Test default values are set
    Assert.assertEquals(2, Config.getAsInt(DockerTest.CFG_NUM_CONTAINERS));
    // Test that system properties are searched if defaults aren't set
    System.setProperty("x.y.z", "5");
    Assert.assertEquals(5, Config.getAsInt("x.y.z"));

    // Test values with no key match don't go sideways
    Assert.assertEquals(0, Config.getAsInt("no.such"));
    Assert.assertEquals(0L, Config.getAsTime("no.such", TimeUnit.SECONDS));
    Assert.assertNull(Config.getAsClass("no.such", getClass()));
    Assert.assertNull(Config.getAsString("no.such"));

    // Test getAsClass
    Config.set("test.get.as.class", getClass().getName());
    Class<? extends TestConfig> tc = Config.getAsClass("test.get.as.class", TestConfig.class);
    Assert.assertEquals(TestConfig.class, tc);
  }

  @Test(expected = IOException.class)
  public void testBadClass() throws IOException {
    Config.set("test.bad.class", "NoSuchClass");
    Utils.getInstance(Config.getAsClass("test.bad.class", ContainerCommand.class));
  }
}
