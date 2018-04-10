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
package org.apache.hive.testutils.dtest;

import java.io.IOException;
import java.util.List;

/**
 * A class to build a ContainerCommand.  If you want to build your own ContainerCommand implement
 * an instance of this that returns that type of command
 */
public abstract class ContainerCommandFactory {
  static ContainerCommandFactory get(String factoryClassName) throws IOException {
    if (factoryClassName == null) factoryClassName = MvnCommandFactory.class.getName();

    Class<? extends ContainerCommandFactory> clazz = Utils.getClass(factoryClassName,
        ContainerCommandFactory.class);
    return Utils.newInstance(clazz);
  }

  /**
   * Return the set of commands that should be run in containers.  One container will be spawned
   * for each container.
   * @param baseDir base directory for operations in the container
   * @return list of commands
   * @throws IOException unable to generate command list.
   */
  public abstract List<ContainerCommand> getContainerCommands(String baseDir) throws IOException;
}
