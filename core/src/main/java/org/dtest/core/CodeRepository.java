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

import java.util.List;

public interface CodeRepository {

  /**
   * Pass in the argument from the command line for the build system.
   * @param arg command line input
   */
  void setCmdlineArg(String arg);

  /**
   * Get the list of commands that should be executed during image creation to checkout the appropriate source code
   * @return list of shell commands
   */
  List<String> repoCommands();
}
