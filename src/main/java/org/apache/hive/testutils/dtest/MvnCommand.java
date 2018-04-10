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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MvnCommand implements ContainerCommand {

  private final String baseDir; // base directory for all commands
  private final String dir; // directory for this particular command
  private String test; // lone test to run
  private List<String> excludes; // tests to exclude
  private String qFilePattern; // qfilePattern to pass
  private Map<String, String> properties; // properties to pass with -D on the command line

  MvnCommand(String baseDir, String dir) {
    this.baseDir = baseDir;
    this.dir = dir;
    this.excludes = new ArrayList<>();
    this.properties = new HashMap<>();
  }

  MvnCommand setTest(String test) {
    this.test = test;
    return this;
  }

  MvnCommand addExclude(String exclude) {
    this.excludes.add(exclude);
    return this;
  }

  MvnCommand setqFilePattern(String qFilePattern) {
    this.qFilePattern = qFilePattern;
    return this;
  }

  MvnCommand addProperty(String key, String value) {
    this.properties.put(key, value);
    return this;
  }

  @Override
  public String containerName() {
    StringBuilder buf = new StringBuilder(dir.replace("/", "-"));
    if (test != null) buf.append("_").append(test);
    if (qFilePattern != null) {
      buf.append("_")
          .append(qFilePattern.replace("[", "_LF_")
              .replace("]", "_RT_")
              .replace("\\", "")
              .replace("*", "_S_"));
    }
    return buf.toString();
  }

  @Override
  public String[] shellCommand() {
    String[] cmd = new String[3];
    cmd[0] = "/bin/bash";
    cmd[1] = "-c";
    StringBuilder buf = new StringBuilder("( cd ")
        .append(baseDir + File.separatorChar + dir)
        .append("; /usr/bin/mvn test -Dsurefire.timeout=3600");
    if (test != null) {
      buf.append(" -Dtest=")
          .append(test);
    }
    if (!excludes.isEmpty()) {
      buf.append(" -Dtest=!");
      boolean first = true;
      for (String exclude : excludes) {
        if (first) first = false;
        else buf.append(",");
        buf.append(exclude);
      }
    }
    if (qFilePattern != null) {
      buf.append(" -Dqfile_regex=")
          .append(qFilePattern);
    }
    for (Map.Entry<String, String> e : properties.entrySet()) {
      buf.append(" -D")
          .append(e.getKey())
          .append("=")
          .append(e.getValue());
    }
    buf.append(" -DskipSparkTests)");
    cmd[2] = buf.toString();
    return cmd;
  }
}
