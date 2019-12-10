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

import org.apache.commons.lang3.StringUtils;
import org.dtest.core.impl.StreamPumper;
import org.dtest.core.testutils.TestLogger;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ITestCmdLine {
  private static final Logger LOG = LoggerFactory.getLogger(ITestCmdLine.class);
  private static final String RESOURCE_DIR = "dtest.itest.resource.dir";
  private static final String PROFILE_YAML = "master.yaml";

  @Test
  public void dogfood() throws IOException, InterruptedException {

    // Link the master.yaml in our source tree to our distribution so we can use it.
    Path profileYamlInDTestHome = Paths.get(System.getProperty("dtest.home"), "conf", PROFILE_YAML);
    Path profileYamlInSourceTree = Paths.get(System.getProperty(RESOURCE_DIR), PROFILE_YAML);
    Files.deleteIfExists(profileYamlInDTestHome);
    Files.createLink(profileYamlInDTestHome, profileYamlInSourceTree);
    try {
      String buildDir = System.getProperty("java.io.tmpdir");

      Map<String, String> env = new HashMap<>();
      env.put("DTEST_HOME", System.getProperty("dtest.home"));
      String[] cmd = {env.get("DTEST_HOME") + File.separator + "bin" + File.separator + "dtest", "-d", buildDir, "-p", "master"};
      LOG.info("Going to run: " + StringUtils.join(cmd, " ") + " with environment " +
          StringUtils.join(env, " "));
      String[] envArray = new String[env.size()];
      int i = 0;
      for (Map.Entry<String, String> e : env.entrySet()) envArray[i++] = e.getKey() + "=" + e.getValue();
      Process proc = Runtime.getRuntime().exec(cmd, envArray);
      AtomicBoolean running = new AtomicBoolean(true);
      TestLogger dtestLog = new TestLogger();
      StreamPumper stdout = new StreamPumper(running, proc.getInputStream(), "itest", dtestLog);
      StreamPumper stderr = new StreamPumper(running, proc.getErrorStream(), "itest", dtestLog);
      new Thread(stdout).start();
      new Thread(stderr).start();
      try {
        Assert.assertTrue(proc.waitFor(300, TimeUnit.SECONDS));
      } finally {
        running.set(false);
        stdout.finalPump();
        stderr.finalPump();
      }
      LOG.info("output:");
      dtestLog.dumpToLog();
      Assert.assertEquals(0, proc.exitValue());
    } finally {
      Files.deleteIfExists(profileYamlInDTestHome);
    }

  }
}
