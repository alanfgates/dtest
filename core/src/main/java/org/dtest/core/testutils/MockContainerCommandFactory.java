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
package org.dtest.core.testutils;

import org.dtest.core.BuildInfo;
import org.dtest.core.ContainerClient;
import org.dtest.core.ContainerCommand;
import org.dtest.core.ContainerCommandFactory;

import java.util.List;

public class MockContainerCommandFactory extends ContainerCommandFactory {
  private final List<ContainerCommand> passedInCommands;

  public MockContainerCommandFactory(List<ContainerCommand> passedInCommands) {
    this.passedInCommands = passedInCommands;
  }

  @Override
  public void buildContainerCommands(ContainerClient containerClient, BuildInfo buildInfo) {
    cmds.addAll(passedInCommands);
  }

  @Override
  public List<String> getInitialBuildCommand() {
    return null;
  }

  @Override
  public List<String> getRequiredPackages() {
    return null;
  }
}
