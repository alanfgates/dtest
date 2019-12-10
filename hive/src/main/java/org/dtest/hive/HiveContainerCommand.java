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

import org.dtest.core.ModuleDirectory;
import org.dtest.core.mvn.MavenContainerCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * Hive specialization of MavenContainerCommand.  This provides a a way to add qfiles to the list of tests to run
 * and provides a subclass of MavenCommandSupplier.
 */
public class HiveContainerCommand extends MavenContainerCommand {
  private boolean isITest;
  private List<String> qfiles; // set of qfiles to run

  /**
   *
   * @param moduleDir information for this directory, must be an instance of {@link HiveModuleDirectory}.
   * @param baseDir working directory on the build machine.
   * @param cmdNumber command number for this command, used in logging and labeling containers.
   */
  HiveContainerCommand(ModuleDirectory moduleDir, String baseDir, int cmdNumber) {
    super(moduleDir, baseDir, cmdNumber);
    qfiles = new ArrayList<>();
    isITest = false;
  }

  /**
   * Add a qfile to the list of tests to be run.  This should only be called if the test is running one of
   * the Test*Driver classes.
   * @param qfile qfile to add.
   */
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
    return new HiveMvnCommandSupplier();
  }

  private class HiveMvnCommandSupplier extends MavenCommandSupplier {
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
