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
import org.dtest.core.impl.Utils;

import java.io.IOException;
import java.util.List;

/**
 * CodeSource controls how code is checked out from the source.
 */
public abstract class CodeSource extends Configurable {

  /**
   * Class that will be used to communicate with the code repository.  Defaults to git.
   */
  public static final String CFG_CODESOURCE_IMPL = "dtest.core.codesource.impl";

  /**
   * Repository the code is stored in.  You must provide a value for this.
   */
  public static final String CFG_CODESOURCE_REPO = "dtest.core.codesource.repo";

  /**
   * Branch in the source tree to build.  Defaults to what makes sense for the chosen VCS, e.g.
   * for git it defaults to 'master'.  This need not be a branch, it can be a tag, a git hash,
   * whatever makes sense for your VCS.
   */
  public static final String CFG_CODESOURCE_BRANCH = "dtest.core.codesource.branch";

  /**
   * Get the list of commands that should be executed during image creation to checkout the appropriate source code
   * @param client container client instance
   * @return list of shell commands
   */
  public abstract List<String> srcCommands(ContainerClient client) throws IOException;

  /**
   * A list of package names that must be included in the container image in order for this source control system
   * to work.
   * @return list of required packages
   */
  public abstract List<String> getRequiredPackages();

  /**
   * Return the name of the default branch.  If the user does not provide a branch in the configuration this will be
   * used.
   * @return default branch
   */
  public abstract String getDefaultBranch();

  static CodeSource getInstance(Config cfg) throws IOException {
    CodeSource cs = Utils.getInstance(cfg.getAsClass(CodeSource.CFG_CODESOURCE_IMPL, CodeSource.class, GitSource.class));
    cs.setConfig(cfg);
    return cs;
  }
}
