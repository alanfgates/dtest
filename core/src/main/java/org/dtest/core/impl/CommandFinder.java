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
package org.dtest.core.impl;

import com.google.common.annotations.VisibleForTesting;
import org.dtest.core.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A singleton used to find commands used by build system, such as docker.  This should only be used for tools on
 * the build machine, not for tools in the container.
 * <p>This tool is simple
 * minded and just looks in PATH, or if PATH is unset, common locations (/usr/local/bin, /usr/bin, /bin)
 * plus any configured locations.  It will use the first instance it finds.  It caches the results to avoid
 * repeatedly searching the box for commands.
 */
public class CommandFinder {

  /**
   * Additional paths to look in beyond whatever is defined in the PATH environment variable.  Defaults to nothing.
   */
  public static final String CFG_COMMANDFINDER_ADDITIONALPATH = "dtest.commandfinder.additionalpath";
  private static final String CFG_COMMANDFINDER_ADDITIONALPATH_DEFAULT = "";

  private static CommandFinder self;

  public static CommandFinder get(Config cfg) {
    if (self == null) {
      synchronized (CommandFinder.class) {
        if (self == null) {
          self = new CommandFinder(cfg);
        }
      }
    }
    return self;
  }

  private Map<String, String> cmds;
  private List<String> path;

  private CommandFinder(Config cfg) {
    cmds = new ConcurrentHashMap<>();
    String p = System.getenv("PATH");
    if (p == null || p.isEmpty()) {
      path = Arrays.asList("/usr/local/bin", "/usr/bin", "/bin");
    } else {
      path = Arrays.asList(p.split(":"));
    }
    String additionalPaths = cfg.getAsString(CFG_COMMANDFINDER_ADDITIONALPATH, CFG_COMMANDFINDER_ADDITIONALPATH_DEFAULT);
    if (!additionalPaths.isEmpty()) {
      path = new ArrayList<>(path);
      path.addAll(Arrays.asList(additionalPaths.split(":")));
    }
  }

  /**
   * Find a command.
   * @param cmd Command to find.  This should match the name of the executable (e.g. mvn).
   * @return Full path to the command, e.g. /usr/bin/mvn.
   */
  public String findCommand(String cmd) {
    return cmds.computeIfAbsent(cmd, s -> {
      for (String p : path) {
        File f = new File(p, s);
        if (f.exists() && f.isFile() && f.canExecute()) return f.getAbsolutePath();
      }
      return null;
    });

  }

  /**
   * Use only for testing.  This forces a new instance of the singleton to be created.
   */
  @VisibleForTesting
  static void forceReset() {
    self = null;
  }


}
