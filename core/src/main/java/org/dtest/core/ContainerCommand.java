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

public interface ContainerCommand {

  /**
   * Get a unique suffix for the container name for this command.  This must return the same value
   * every time for a given instance.  The name must be unique, and it must be a valid docker
   * container name (generally means it uses only letters, numbers, underscore, and dash).
   * @return unique name.
   */
  String containerSuffix();

  /**
   * Build a shell command.  The command itself should be the first element in the array, with
   * any arguments as subsequent elements.
   * @return the command.
   */
  String[] shellCommand();

  /**
   * Get the directory the container will execute in.
   * @return container directory
   */
  String containerDirectory();

}