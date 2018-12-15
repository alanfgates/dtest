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

import org.dtest.core.impl.Utils;
import org.dtest.core.BaseContainerCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class HiveContainerCommand extends BaseContainerCommand {
  private boolean isITest;
  private List<String> qfiles; // set of qfiles to run

  public HiveContainerCommand(String baseDir, int cmdNumber) {
    super(baseDir, cmdNumber);
    qfiles = new ArrayList<>();
    isITest = false;
  }

  void addQfile(String qfile) {
    qfiles.add(qfile);
    isITest = true;
  }

  @Override
  public String containerSuffix() {
    return isITest ? super.containerSuffix() : "itest-" + cmdNumber;
  }

  @Override
  public String[] shellCommand() {
    if (isITest) assert tests.size() == 1;
    return Utils.shellCmdInRoot(baseDir, new HiveMvnCommandSupplier());
  }


  private class HiveMvnCommandSupplier implements Supplier<String> {
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
            buf.append(qfile.trim());
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
