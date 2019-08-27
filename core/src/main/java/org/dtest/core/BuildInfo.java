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
import org.dtest.core.impl.Utils;

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

  private final Pattern dockerable = Pattern.compile("[a-z0-9_\\-]+");
  private final CodeSource src;
  private final boolean cleanupAfter;
  private final BuildYaml yaml;
  private String buildDirName;
  private File buildDir; // Directory the build will be done in
  private String label;

  /**
   *
   * @param yaml Yaml file object
   * @param repo code source object that will be used to fetch code.
   * @param cleanupAfter whether we should cleanup after this build
   * @param buildDir Directory for this build.
   */
  public BuildInfo(BuildYaml yaml, CodeSource repo, boolean cleanupAfter, String buildDir) {
    this.src = repo;
    this.yaml = yaml;
    this.cleanupAfter = cleanupAfter;
    buildDirName = buildDir;
  }

  /**
   * Get the directory for this build.  This is the directory on the build machine that will be the working directory
   * for the build.  The dtest.log for the build will be in this directory.  It is constructed by using
   * baseDir/labelname.  If the directory does not exist this call will create it.
   * You must call {@link #setConfig(Config)} before calling this.
   * @return directory
   * @throws IOException if the directory can't be built.
   */
  public File getBuildDir() throws IOException {
    if (buildDir != null) return buildDir;

    // This cannot be done in the constructor because it requires the configuration.
    buildDir = new File(getBuildDirName());
    buildDir.mkdir();
    log.info("Build dir for build is " + buildDir.getAbsolutePath());
    return buildDir;
  }

  private String getBuildDirName() {
    if (buildDirName == null) {
      buildDirName = cfg.getAsString(CFG_BUILDINFO_BASEDIR);
      if (buildDirName == null) {
        buildDirName = System.getProperty("java.io.tmpdir");
      }
    }
    return buildDirName;
  }


  /**
   * Get the object that controls how the code is checked out from source.
   * @return code source
   */
  public CodeSource getSrc() {
    return src;
  }

  /**
   * Get the label for the build.
   * @return build label.
   */
  public String getLabel() throws IOException {
    // Don't check validity of label here, we'll do that when we construct the build buildDir.
    if (label == null) {
      label = cfg.getAsString(CFG_BUILDINFO_LABEL);
      if (label == null) {
        label = Utils.generateLabel(getYaml().getBranch());
      }
      checkLabelIsDockerable();
      log.info("Using label " + label);
    }
    return label;
  }

  /**
   * Whether the system should cleanup after the build.  Defaults to true, but can be set via the command line
   * to false for debugging.
   * @return whether to clean up after the build.
   */
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

  public BuildYaml getYaml() {
    return yaml;
  }

  @VisibleForTesting
  void checkLabelIsDockerable() throws IOException {
    if (label == null) {
      label = getLabel();
      Matcher m = dockerable.matcher(label);
      if (!m.matches()) {
        throw new IOException("Label '" + label + "' must be usable in docker container name, should only contain " +
            "[A-Za-z0-9_\\-]");
      }
    }
  }

}
