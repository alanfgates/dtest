/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hive.testutils.dtest.impl;

import org.apache.hive.testutils.dtest.Config;
import org.apache.hive.testutils.dtest.ContainerCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class MvnCommand implements ContainerCommand {

  private final String baseDir; // base directory for all commands
  private final int cmdNumber;
  private boolean isITest;
  private List<String> tests; // set of tests to run
  private List<String> excludedTests; // set of tests to NOT run
  private List<String> qfiles; // set of qfiles to run
  private Map<String, String> envs;
  private long testTimeout;

  MvnCommand(String baseDir, int cmdNumber) {
    this.baseDir = baseDir;
    this.cmdNumber = cmdNumber;
    tests = new ArrayList<>();
    excludedTests = new ArrayList<>();
    qfiles = new ArrayList<>();
    envs = new HashMap<>();
    int testTimeProperty = Integer.valueOf(System.getProperty(Config.TEST_RUN_TIME, "60"));
    TimeUnit testTimeUnit = TimeUnit.valueOf(System.getProperty(Config.TEST_RUN_TIME_UNIT, "MINUTES"));
    testTimeout = TimeUnit.SECONDS.convert(testTimeProperty, testTimeUnit);
    isITest = false;
  }

  MvnCommand addTest(String test) {
    tests.add(test);
    return this;
  }

  MvnCommand excludeTest(String test) {
    excludedTests.add(test);
    return this;
  }

  MvnCommand addQfile(String qfile) {
    qfiles.add(qfile);
    isITest = true;
    return this;
  }

  MvnCommand setEnv(String envVar, String value) {
    envs.put(envVar, value);
    return this;
  }

  @Override
  public String containerSuffix() {
    return (isITest ? "itest" : "unittest") + "-" + cmdNumber;
  }

  @Override
  public String[] shellCommand() {
    if (isITest) assert tests.size() == 1;
    return Utils.shellCmdInRoot(baseDir, new MvnCommandSupplier());
  }

  @Override
  public String containerDirectory() {
    return baseDir;
  }

  private class MvnCommandSupplier implements Supplier<String> {
    public String get() {
      StringBuilder buf = new StringBuilder();
      for (Map.Entry<String, String> e : envs.entrySet()) {
        buf.append(e.getKey())
            .append('=')
            .append(e.getValue())
            .append(' ');
      }

      buf.append("/usr/bin/mvn test -Dsurefire.timeout=")
          .append(testTimeout);

      if (!tests.isEmpty()) {
        buf.append(" -Dtest=");
        boolean first = true;
        for (String test : tests) {
          if (first) first = false;
          else buf.append(',');
          buf.append(test);
        }
        if (isITest) {
          buf.append(" -Dqfile=");
          first = true;
          for (String qfile : qfiles) {
            if (first) first = false;
            else buf.append(',');
            buf.append(qfile);
          }
        }
      }
      if (!excludedTests.isEmpty()) {
        buf.append(" -Dtest.excludes.additional=");
        boolean first = true;
        for (String excludedTest : excludedTests) {
          if (first) first = false;
          else buf.append(',');
          buf.append("**/")
            .append(excludedTest);
        }
      }
      buf.append(" -Dtest.groups=\"\" -DskipSparkTests");
      return buf.toString();
    }
  }
}
