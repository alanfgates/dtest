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

/**
 * Track information about the build.
 */
public class BuildInfo extends Configurable implements Comparable<BuildInfo> {
  /**
   * Directory to run the build in.  This is the directory on the base machine, not in the container.
   * This value must be set.
   */
  public static final String CFG_BUILDINFO_BASEDIR = "dtest.core.buildinfo.basedir";

  /**
   * Label for this build.  This value must be set.  Usually you want this to be unique, as this is used
   * for the docker image label.  You can force a new build from scratch on the same repo and branch/hash by
   * changing this label.
   */
  public static final String CFG_BUILDINFO_LABEL = "dtest.core.buildinfo.label";

  private final Pattern dockerable = Pattern.compile("[A-Za-z0-9_\\-]+");
  private final CodeSource src;
  private final String confDir;
  private final boolean cleanupAfter;
  private String label;
  private String dir;

  /**
   *
   * @param confDir directory where we expect to find dtest.properties file
   * @param repo code source object that will be used to fetch code.
   * @param cleanupAfter whether we should cleanup after this build
   */
  public BuildInfo(String confDir, CodeSource repo, boolean cleanupAfter) {
    this.confDir = confDir;
    this.src = repo;
    dir = null;
    this.cleanupAfter = cleanupAfter;
  }

  /**
   * Get the directory for this build.  This is the directory on the build machine that will be used as the
   * base for the build.  If the directory does not exist this call will create it.
   * You must call {@link #setConfig(Config)} before calling this.
   * @return directory name
   * @throws IOException if the directory can't be built.
   */
  public String getBuildDir() throws IOException {
    if (dir != null) return dir;
    // This cannot be done in the constructor because it requires the configuration.
    checkLabelIsDockerable();
    File d = new File(getBaseDir(), label);
    d.mkdir();
    dir = d.getAbsolutePath();
    return dir;
  }

  /**
   * Get the base directory for this build.
   * @return base directory
   * @throws IOException if basedir isn't provided in the configuration.
   */
  public String getBaseDir() throws IOException {
    String baseDir = cfg.getAsString(CFG_BUILDINFO_BASEDIR);
    if (baseDir == null) throw new IOException(CFG_BUILDINFO_BASEDIR + " not set, required");
    return baseDir;
  }


  /**
   * Get the object that controls how the code is checked out from source
   * @return code source
   */
  public CodeSource getSrc() {
    return src;
  }

  public String getLabel() {
    // Don't check validity of label here, we'll do that when we construct the build dir.
    if (label == null) {
      label = cfg.getAsString(CFG_BUILDINFO_LABEL);
    }
    return label;
  }

  /**
   * Get the configuration directory for dtest.
   * @return dtest conf dir
   */
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
  void checkLabelIsDockerable() throws IOException {
    if (label == null) {
      label = getLabel();
      if (label == null) throw new IOException("You must specify a build label using " + CFG_BUILDINFO_LABEL);
      Matcher m = dockerable.matcher(label);
      if (!m.matches()) {
        throw new IOException("Label must be usable in docker container name, should only contain " +
            "[A-Za-z0-9_\\-]");
      }
    }
  }

}
