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

import org.dtest.core.impl.Utils;

import java.io.IOException;
import java.util.List;

/**
 * A class to build a ContainerCommand.  If you want to build your own ContainerCommand implement
 * an instance of this that returns that type of command
 */
public abstract class ContainerCommandFactory {

  static ContainerCommandFactory get() throws IOException {
    Class<? extends ContainerCommandFactory> clazz =
        Config.CONTAINER_COMMAND_FACTORY.getAsClass(ContainerCommandFactory.class);
    return Utils.newInstance(clazz);
  }

  /**
   * Return the set of commands that should be run in containers.  One container will be spawned
   * for each container.
   * @param containerClient container client, in case any containers are needed for determining
   *                        commands to run.
   * @param buildInfo information for this build
   * @param logger logger
   * @return list of commands
   * @throws IOException unable to generate command list.
   */
  public abstract List<ContainerCommand> getContainerCommands(ContainerClient containerClient,
                                                              BuildInfo buildInfo,
                                                              DTestLogger logger)
      throws IOException;
}
