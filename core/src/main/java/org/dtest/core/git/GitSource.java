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

import com.google.common.annotations.VisibleForTesting;
import org.dtest.core.CodeSource;
import org.dtest.core.Config;
import org.dtest.core.ContainerClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GitSource extends CodeSource  {

  @VisibleForTesting
  public static final String CFG_GIT_REPO = "dtest.git.repo";
  public static final String CFG_GIT_BRANCH = "dtest.git.branch";

  @Override
  public List<String> srcCommands(ContainerClient client) throws IOException {
    String repo = Config.getAsString(CFG_GIT_REPO);
    String branch = Config.getAsString(CFG_GIT_BRANCH);
    if (repo == null || branch == null) {
      throw new IOException("You must provide configuration values " + CFG_GIT_REPO + " and " + CFG_GIT_BRANCH +
          " to use git");
    }
    return Arrays.asList(
        "    /usr/bin/git clone " + repo + "; \\\n",
        "    cd " + client.getProjectName() + "; \\\n",
        "    /usr/bin/git checkout " + branch + "; \\\n");
  }
}
