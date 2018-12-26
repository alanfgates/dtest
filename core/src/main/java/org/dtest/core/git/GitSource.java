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
package org.dtest.core.git;

import org.dtest.core.CodeSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GitSource extends CodeSource  {

  private static final String CFG_CODESOURCE_BRANCH_DEFAULT = "master";

  @Override
  public List<String> srcCommands(String projectDir) throws IOException {
    String repo = cfg.getAsString(CFG_CODESOURCE_REPO);
    String branch = cfg.getAsString(CFG_CODESOURCE_BRANCH, CFG_CODESOURCE_BRANCH_DEFAULT);
    if (repo == null) {
      throw new IOException("You must provide configuration value " + CFG_CODESOURCE_REPO + " to use git");
    }
    return Arrays.asList(
        "    /usr/bin/git clone " + repo,
        "    cd " + projectDir,
        "    /usr/bin/git checkout " + branch);
  }

  @Override
  public List<String> getRequiredPackages() {
    return Collections.singletonList("git");
  }

  @Override
  public String getDefaultBranch() {
    return "master";
  }
}
