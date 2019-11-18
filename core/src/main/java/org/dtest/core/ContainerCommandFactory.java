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
import org.dtest.core.mvn.MavenContainerCommandFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ContainerCommandFactory builds a list of {@link ContainerCommand}s.  It also provides information for
 * building the initial image.
 */
public abstract class ContainerCommandFactory extends Configurable {

  /**
   * Number of tests to run per container.  Defaults to 10.
   */
  public static final String CFG_CONTAINERCOMMANDFACTORY_TESTSPERCONTAINER = "dtest.core.containercommandfactory.testspercontainer";
  protected static final int CFG_CONTAINERCOMMANDFACTORY_TESTSPERCONTAINER_DEFAULT = 10;

  /**
   * Implementation that builds commands to run in the container.  Defaults to simple maven builder.
   */
  public static final String CFG_CONTAINERCOMMANDLIST_IMPL = "dtest.core.containercommandfactory.impl";

  protected List<ContainerCommand> cmds = new ArrayList<>();

  /**
   * Build the list of commands.
   * @param containerClient container client, in case any containers are needed for determining
   *                        commands to run.
   * @param buildInfo information for this build
   * @throws IOException unable to generate command list.
   */
  public abstract void buildContainerCommands(ContainerClient containerClient, BuildInfo buildInfo)
      throws IOException;

  /**
   * Get the initial build command for the project.  This will be placed in the Dockerfile after source control
   * checkout as part of image creation.
   * @return initial commands to run.
   */
  public abstract List<String> getInitialBuildCommand();

  /**
   * Get a list of packages required to use the commands in this factory.  These are rpm or deb packages that need
   * to be installed in the container as part of the image.
   * @return list of packages
   */
  public abstract List<String> getRequiredPackages();

  /**
   * Get the list of commands.  This must be called after
   * {@link #buildContainerCommands(ContainerClient, BuildInfo)}.
   * @return list of commands.
   */
  public List<ContainerCommand> getCmds() {
    return cmds;
  }

  /**
   * Get a list of additional commands to run in the Docker image build.  These will be run after all the packages
   * have been installed and before the user is added.  This is useful for installing packages that don't have a
   * rpm or deb package, or where you need to custom build them for whatever reason.
   * @return list of commands to run.
   */
  public List<String> getAdditionalDockerBuildCommands() {
    return Collections.emptyList();
  }

  static ContainerCommandFactory getInstance(Config cfg, DTestLogger log) throws IOException {
    ContainerCommandFactory ccl = Utils.getInstance(cfg.getAsClass(ContainerCommandFactory.CFG_CONTAINERCOMMANDLIST_IMPL,
        ContainerCommandFactory.class, MavenContainerCommandFactory.class));
    log.debug("Instantiating ContainerCommandFactory implementation " + ccl.getClass().getName());
    ccl.setConfig(cfg).setLog(log);
    return ccl;
  }
}
