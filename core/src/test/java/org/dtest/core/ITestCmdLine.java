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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.dtest.core.git.GitSource;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ITestCmdLine {
  private static final Logger LOG = LoggerFactory.getLogger(ITestCmdLine.class);

  @Test
  public void dogfood() throws IOException, InterruptedException {

    String confDir = System.getProperty("conf.dir") + File.separator + "test-classes" + File.separator + "itest";

    Map<String, String> env = new HashMap<>();
    env.put("DTEST_HOME", System.getProperty("dtest.home"));
    String[] cmd = {env.get("DTEST_HOME") + File.separator + "bin" + File.separator + "dtest",
                    "-c", confDir,
                    "-D" + BuildInfo.CFG_BUILDINFO_LABEL + "=" + RandomStringUtils.randomAlphanumeric(21).toLowerCase(),
                    "-D" + BuildInfo.CFG_BUILDINFO_BASEDIR + "=" + confDir};
    LOG.info("Going to run: " + StringUtils.join(cmd, " ") + " with environment " +
        StringUtils.join(env, " "));
    String[] envArray = new String[env.size()];
    int i = 0;
    for (Map.Entry<String, String> e : env.entrySet()) envArray[i++] = e.getKey() + "=" + e.getValue();
    Process proc = Runtime.getRuntime().exec(cmd, envArray);
    BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
    final StringBuilder lines = new StringBuilder();
    reader.lines()
        .forEach(s -> lines.append(s).append('\n'));

    reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
    final StringBuilder errLines = new StringBuilder();
    reader.lines()
        .forEach(s -> errLines.append(s).append('\n'));
    LOG.info("stdout: " + lines.toString());
    LOG.info("stderr: " + errLines.toString());
    Assert.assertTrue(proc.waitFor(300, TimeUnit.SECONDS));
    Assert.assertEquals(0, proc.exitValue());

  }
}
