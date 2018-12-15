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

import org.dtest.core.Config;
import org.dtest.core.ContainerCommand;
import org.dtest.core.impl.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class BaseContainerCommand implements ContainerCommand {

  protected final String baseDir; // base directory for all commands
  protected final int cmdNumber;
  protected List<String> tests; // set of tests to run
  protected List<String> excludedTests; // set of tests to NOT run
  protected Map<String, String> envs;
  protected Map<String, String> properties; // properties to pass to maven (-DX=Y) val can be null
  protected long testTimeout;

  public BaseContainerCommand(String baseDir, int cmdNumber) {
    this.baseDir = baseDir;
    this.cmdNumber = cmdNumber;
    tests = new ArrayList<>();
    excludedTests = new ArrayList<>();
    envs = new HashMap<>();
    properties = new HashMap<>();
    testTimeout = Config.TEST_RUN_TIME.getAsTime(TimeUnit.SECONDS);
  }

  public void addTest(String test) {
    tests.add(test);
  }

  public void excludeTests(String[] toExclude) {
    Collections.addAll(excludedTests, toExclude);
  }

  public void setEnv(String envVar, String value) {
    envs.put(envVar, value);
  }

  public void addEnvs(Map<String, String> envs) {
    this.envs.putAll(envs);
  }

  public void addProperties(Map<String, String> props) {
    properties.putAll(props);
  }

  @Override
  public String containerSuffix() {
    return "unittest-" + cmdNumber;
  }

  @Override
  public String[] shellCommand() {
    return Utils.shellCmdInRoot(baseDir, new SimpleMvnCommandSupplier());
  }

  @Override
  public String containerDirectory() {
    return baseDir;
  }

  private class SimpleMvnCommandSupplier implements Supplier<String> {
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
      for (Map.Entry<String, String> e : properties.entrySet()) {
        buf.append(" -D")
            .append(e.getKey());
        if (e.getValue() != null) {
          buf.append('=')
              .append(e.getValue());
        }
      }
      return buf.toString();
    }
  }
}
