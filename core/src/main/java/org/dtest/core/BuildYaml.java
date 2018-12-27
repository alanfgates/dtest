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

import java.io.IOException;

/**
 * BuildYaml is the top level file for holding the object described in the build.yaml file.  It contains general
 * build information as well as an array of ModuleDirectory.  Implementations can override this class in order to
 * get information specific to their build.  They can add top level information or subclass
 * {@link ModuleDirectory} in order to add additional information in each module.  If an implementation subclasses
 * ModuleDirectory it will also need to subclass {@link org.dtest.core.mvn.MavenContainerCommandFactory} and
 * implement {@link org.dtest.core.mvn.MavenContainerCommandFactory#getModuleDirs(BuildYaml)}.
 */
public class BuildYaml {

  /**
   * Implementation of BuildYaml to use to interpret build.yaml.  Defaults to BuildYaml.
   */
  public static final String CFG_BUILDYAML_IMPL = "dtest.core.buildyaml.impl";
  private static final Class<? extends BuildYaml> CFG_BUILDYAML_IMPL_DEFAULT = BuildYaml.class;

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  // TODO - figure out if javadoc will pick these up or I need to change them to another privacy level.
  /**
   * Base image to use.  Currently supported values are centos, ubuntu, or debian.  These can also include
   * a version number if desired.
   */
  private String baseImage;

  /**
   * Required packages that should be installed for your build to work.
   */
  private String[] requiredPackages;

  /**
   * Name of the project.
   */
  private String projectName;

  /**
   * List of modules to run tests in.
   */
  private ModuleDirectory[] dirs;

  /**
   * Not used by the system.  Your chance to ad lib.
   */
  private String comment;

  /**
   * Top level Java packages that the tests are in.  These are not the individual modules but top level ones, such
   * as org.apache.hadoop or org.dtest.  Done as an array because some projects have tests in multiple top level
   * packages, e.g. Apache Hive has org.apache.hadoop.hive and org.apache.hive.
   */
  private String[] javaPackages;

  /**
   * Any additional log files that should be picked up as part of the collection of log files to ship back to
   * the user.
   */
  private String[] additionalLogs;

  static Class<? extends BuildYaml> getBuildYamlClass(Config cfg) throws IOException {
    return cfg.getAsClass(CFG_BUILDYAML_IMPL, BuildYaml.class, CFG_BUILDYAML_IMPL_DEFAULT);
  }

  public String getBaseImage() {
    return baseImage;
  }

  public void setBaseImage(String baseImage) {
    this.baseImage = baseImage;
  }

  public String[] getRequiredPackages() {
    return requiredPackages == null ? EMPTY_STRING_ARRAY : requiredPackages;
  }

  public void setRequiredPackages(String[] requiredPackages) {
    this.requiredPackages = requiredPackages;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public ModuleDirectory[] getDirs() {
    return dirs;
  }

  public void setDirs(ModuleDirectory[] dirs) {
    this.dirs = dirs;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String[] getJavaPackages() {
    return javaPackages == null ? EMPTY_STRING_ARRAY : javaPackages;
  }

  public void setJavaPackages(String[] javaPackages) {
    this.javaPackages = javaPackages;
  }

  public String[] getAdditionalLogs() {
    return additionalLogs == null ? EMPTY_STRING_ARRAY : additionalLogs;
  }

  public void setAdditionalLogs(String[] additionalLogs) {
    this.additionalLogs = additionalLogs;
  }
}
