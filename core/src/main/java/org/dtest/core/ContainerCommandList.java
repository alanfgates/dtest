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

import com.google.common.annotations.VisibleForTesting;
import org.dtest.core.impl.PluginFactory;

import java.io.IOException;
import java.util.ArrayList;

public abstract class ContainerCommandList extends ArrayList<ContainerCommand> {
  @VisibleForTesting
  // Implementation of ContainerCommandList
  public static final String CFG_CONTAINER_COMMAND_LIST = "dtest.container.command.list";

  static {
    Config.setDefaultValue(CFG_CONTAINER_COMMAND_LIST, BaseContainerCommandList.class.getName());
  }
  /**
   * Build the list of commands.
   * @param containerClient container client, in case any containers are needed for determining
   *                        commands to run.
   * @param buildInfo information for this build
   * @param logger logger
   * @throws IOException unable to generate command list.
   */
  public abstract void buildContainerCommands(ContainerClient containerClient, BuildInfo buildInfo, DTestLogger logger)
      throws IOException;

  static ContainerCommandList getInstance() throws IOException {
    return PluginFactory.getInstance(Config.getAsClass(
        ContainerCommandList.CFG_CONTAINER_COMMAND_LIST, ContainerCommandList.class));

  }
}
