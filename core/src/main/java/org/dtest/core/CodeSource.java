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

import org.dtest.core.git.GitSource;
import org.dtest.core.impl.PluginFactory;

import java.io.IOException;
import java.util.List;

public abstract class CodeSource {

  private static final String CFG_CODE_SOURCE_CLASS = "dtest.code.source.class";

  static {
    Config.setDefaultValue(CFG_CODE_SOURCE_CLASS, GitSource.class.getName());
  }

  /**
   * Get the list of commands that should be executed during image creation to checkout the appropriate source code
   * @param client container client instance
   * @return list of shell commands
   */
  public abstract List<String> srcCommands(ContainerClient client) throws IOException;

  static CodeSource getInstance() throws IOException {
    return PluginFactory.getInstance(Config.getAsClass(CodeSource.CFG_CODE_SOURCE_CLASS, CodeSource.class));
  }
}
