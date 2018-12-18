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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TestUtils {

  public static void createConfFile() throws IOException {
    File propertiesFile = new File(getConfDir(), Config.PROPERTIES_FILE);
    propertiesFile.deleteOnExit();
    FileWriter writer = new FileWriter(propertiesFile);
    writer.write("test = test\n");
    writer.close();

  }

  public static String getConfDir() {
    return System.getProperty("java.io.tmpdir") + File.separator + "test-classes";
  }

}
