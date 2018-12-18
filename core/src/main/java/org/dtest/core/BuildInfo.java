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

import com.google.common.annotations.VisibleForTesting;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildInfo implements Comparable<BuildInfo> {
  @VisibleForTesting
  public static final String CFG_BUILD_BASE_DIR = "dtest.build.base.dir";

  private final Pattern dockerable = Pattern.compile("[A-Za-z0-9_\\-]+");
  private final CodeSource src;
  private final String label;
  private final String confDir;
  private String dir;
  private boolean cleanupAfter;

  public BuildInfo(String confDir, CodeSource repo, String label) throws IOException {
    this.confDir = confDir;
    this.src = repo;
    this.label = checkLabelIsDockerable(label);
    dir = null;
    cleanupAfter = true;
  }

  /**
   * Create a directory for this build.
   * @return directory name
   * @throws IOException if the directory can't be built.
   */
  String buildDir() throws IOException {
    if (dir != null) return dir;
    File d = new File(getBaseDir(), label);
    d.mkdir();
    dir = d.getAbsolutePath();
    return dir;
  }

  public String getBaseDir() throws IOException {
    String baseDir = Config.getAsString(CFG_BUILD_BASE_DIR);
    if (baseDir == null) throw new IOException(CFG_BUILD_BASE_DIR + " not set, required");
    return baseDir;
  }

  public CodeSource getSrc() {
    return src;
  }

  public String getLabel() {
    return label;
  }

  public String getDir() {
    return dir;
  }

  public String getConfDir() {
    return confDir;
  }

  public boolean shouldCleanupAfter() {
    return cleanupAfter;
  }

  public void setCleanupAfter(boolean cleanupAfter) {
    this.cleanupAfter = cleanupAfter;
  }

  @Override
  public int hashCode() {
    return label.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BuildInfo)) return false;
    BuildInfo that = (BuildInfo)obj;
    return label.equals(that.label);
  }

  @Override
  public String toString() {
    return "code source: " + src.toString() + (label == null ? "" : ", label: " + label);
  }

  @Override
  public int compareTo(BuildInfo o) {
    return label.compareTo(o.label);
  }

  private String checkLabelIsDockerable(String label) throws IOException {
    Matcher m = dockerable.matcher(label);
    if (m.matches()) {
      return label;
    } else {
      throw new IOException("Label must be usable in docker container name, should only contain " +
          "[A-Za-z0-9_\\-]");
    }
  }
}
