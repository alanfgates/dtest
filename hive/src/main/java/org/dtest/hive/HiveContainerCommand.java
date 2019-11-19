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

import org.dtest.core.mvn.MavenContainerCommand;

import java.util.ArrayList;
import java.util.List;

public class HiveContainerCommand extends MavenContainerCommand {
  private boolean isITest;
  private List<String> qfiles; // set of qfiles to run

  public HiveContainerCommand(String baseDir, String moduleDir, int cmdNumber) {
    super(baseDir, moduleDir, cmdNumber);
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
  protected MavenCommandSupplier getCommandSupplier() {
    return new HiveMvnCommandSupplier(moduleDir);
  }

  private class HiveMvnCommandSupplier extends MavenCommandSupplier {
    private HiveMvnCommandSupplier(String moduleDir) {
      super(moduleDir);
    }

    @Override
    protected void addAdditionalArguments(StringBuilder buf) {
      if (!tests.isEmpty() && isITest) {
        assert tests.size() == 1;
        buf.append(" -Dqfile=");
        boolean first = true;
        for (String qfile : qfiles) {
          if (first) first = false;
          else buf.append(',');
          buf.append(qfile.trim());
        }
      }
    }
  }
}
