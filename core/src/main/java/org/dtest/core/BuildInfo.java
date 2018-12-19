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

public class BuildInfo extends Configurable implements Comparable<BuildInfo> {
  @VisibleForTesting
  public static final String CFG_BUILDINFO_BASEDIR = "dtest.core.buildinfo.basedir";
  public static final String CFG_BUILDINFO_LABEL = "dtest.core.buildinfo.label";

  private final Pattern dockerable = Pattern.compile("[A-Za-z0-9_\\-]+");
  private final CodeSource src;
  private final String confDir;
  private final boolean cleanupAfter;
  private String label;
  private String dir;

  public BuildInfo(String confDir, CodeSource repo, boolean cleanupAfter) throws IOException {
    this.confDir = confDir;
    this.src = repo;
    dir = null;
    this.cleanupAfter = cleanupAfter;
  }

  /**
   * Create a directory for this build.  You must call {@link #setConfig(Config)} before calling this.
   * @return directory name
   * @throws IOException if the directory can't be built.
   */
  String buildDir() throws IOException {
    if (dir != null) return dir;
    this.label = checkLabelIsDockerable();
    File d = new File(getBaseDir(), label);
    d.mkdir();
    dir = d.getAbsolutePath();
    return dir;
  }

  public String getBaseDir() throws IOException {
    String baseDir = getConfig().getAsString(CFG_BUILDINFO_BASEDIR);
    if (baseDir == null) throw new IOException(CFG_BUILDINFO_BASEDIR + " not set, required");
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

  @VisibleForTesting
  String checkLabelIsDockerable() throws IOException {
    String label = getConfig().getAsString(CFG_BUILDINFO_LABEL);
    if (label == null) throw new IOException("You must specify a build label using " + CFG_BUILDINFO_LABEL);
    Matcher m = dockerable.matcher(label);
    if (m.matches()) {
      return label;
    } else {
      throw new IOException("Label must be usable in docker container name, should only contain " +
          "[A-Za-z0-9_\\-]");
    }
  }
}
