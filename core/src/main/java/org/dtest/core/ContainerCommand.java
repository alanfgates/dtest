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

/**
 * ContainerCommand constructs commands to be run in a container.
 */
public abstract class ContainerCommand extends Configurable {

  /*~~
   * @document propsfile
   * @section containercommand_singleruntime
   * @after containerclient_imagebuildtime
   * - dtest.core.containercommand.singtestruntime: Maximum amount of runtime for a single test executed inside a
   * container.  If a test exceeds this amount of time
   * the test will be marked as timed out and the build as having timeouts (note that this is different than
   * the build itself timing out).  Default is 5 minutes.
   *
   */
  /**
   * Maximum amount of time for a single command in the container to run.  Defaults to 5 minutes.
   */
  public static final String CFG_CONTAINERCOMMAND_SINGLERUNTIME = "dtest.core.containercommand.singletestruntime";
  protected static final long CFG_CONTAINERCOMMAND_SINGLERUNTIME_DEFAULT = 5 * 60;

  /**
   * Get a unique suffix for the container name for this command.  This must return the same value
   * every time for a given instance.  The name must be unique, and it must be a valid docker
   * container name (generally means it uses only letters, numbers, underscore, and dash).
   * @return unique name.
   */
  public abstract String containerSuffix();

  /**
   * Build a shell command.  The command itself should be the first element in the array, with
   * any arguments as subsequent elements.
   * @return the command.
   */
  public abstract String[] shellCommand();

  /**
   * Get the directory the container will execute in.
   * @return container directory
   */
  public abstract String containerDirectory();

}
