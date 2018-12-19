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
import org.dtest.core.impl.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ContainerCommandList extends Configurable {

  protected List<ContainerCommand> cmds = new ArrayList<>();

  @VisibleForTesting
  // Implementation of ContainerCommandList
  public static final String CFG_CONTAINERCOMMANDLIST_IMPL = "dtest.core.containercommandlist.impl";

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

  public List<ContainerCommand> getCmds() {
    return cmds;
  }

  static ContainerCommandList getInstance(Config cfg) throws IOException {
    ContainerCommandList ccl = Utils.getInstance(cfg.getAsClass(ContainerCommandList.CFG_CONTAINERCOMMANDLIST_IMPL,
        ContainerCommandList.class, BaseContainerCommandList.class));
    ccl.setConfig(cfg);
    return ccl;
  }
}
