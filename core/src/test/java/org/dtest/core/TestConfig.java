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

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TestConfig {

  @Test
  public void testConfigFile() throws IOException {
    File confDir = new File(System.getenv(DockerTest.DTEST_HOME), Config.CONF_DIR);
    confDir.mkdir();
    File propertiesFile = new File(confDir, Config.PROPERTIES_FILE);
    Config.NUMBER_OF_CONTAINERS.set("5");
    try {
      FileWriter writer = new FileWriter(propertiesFile);
      writer.write(Config.CONTAINER_RUN_TIME.getProperty() + " = 2\n");
      // Make sure we don't overwrite existing properites
      writer.write(Config.NUMBER_OF_CONTAINERS.getProperty() + " = 10\n");
      writer.close();

      Config.IMAGE_BUILD_TIME.set("15");
      Config.fromConfigFile();

      Assert.assertEquals(15, Config.IMAGE_BUILD_TIME.getAsInt());
      Assert.assertEquals(2, Config.CONTAINER_RUN_TIME.getAsInt());
      Assert.assertEquals(10, Config.TESTS_PER_CONTAINER.getAsInt());
      Assert.assertEquals(5, Config.NUMBER_OF_CONTAINERS.getAsInt());

      Config.IMAGE_BUILD_TIME.resetValue();
      Config.CONTAINER_RUN_TIME.resetValue();
      Config.TESTS_PER_CONTAINER.resetValue();

      Assert.assertEquals(15 * 60, Config.IMAGE_BUILD_TIME.getAsTime(TimeUnit.SECONDS));
      Assert.assertEquals(2 * 60, Config.CONTAINER_RUN_TIME.getAsTime(TimeUnit.MINUTES));

      Config.IMAGE_BUILD_TIME.resetValue();
      Config.CONTAINER_RUN_TIME.resetValue();
      Config.TESTS_PER_CONTAINER.resetValue();

      Assert.assertEquals("15", Config.IMAGE_BUILD_TIME.getAsString());
      Assert.assertEquals("2", Config.CONTAINER_RUN_TIME.getAsString());
      Assert.assertEquals("10", Config.TESTS_PER_CONTAINER.getAsString());
    } finally {
      propertiesFile.delete();
    }
  }
}
