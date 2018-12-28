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

import org.dtest.documentation.annotation.Document;
import org.dtest.documentation.annotation.DocumentEntry;

import java.io.IOException;

/**
 * BuildYaml is the top level file for holding the object described in the build.yaml file.  It contains general
 * build information as well as an array of ModuleDirectory.  Implementations can override this class in order to
 * get information specific to their build.  They can add top level information or subclass
 * {@link ModuleDirectory} in order to add additional information in each module.  If an implementation subclasses
 * ModuleDirectory it will also need to subclass {@link org.dtest.core.mvn.MavenContainerCommandFactory} and
 * implement {@link org.dtest.core.mvn.MavenContainerCommandFactory#getModuleDirs(BuildYaml)}.
 */
@Document(name = "yamlfile", sections = {"header", "elementlist", "mdelements"})
@DocumentEntry(documentName = "yamlfile", documentSection = "header", text="# YAML Build File\n" +
    "The dtest.yaml file determines how the project is built.  It contains information that defines how to" +
    "build the image (such as the base image and required packages that must be installed) as well as " +
    "information for each set of tests that must be run.\n" +
    "An example dtest.yaml file might look like:\n" +
    "> baseImage: centos\n" +
    "> requiredPackages:\n" +
    ">   - java-1.8.0-openjdk-devel\n" +
    "> projectName: myproject\n" +
    "> javaPackages:\n" +
    "> - org.myproject\n" +
    "> dirs:\n" +
    ">   - dir: core\n" +
    ">   - dir: apps\n" +
    ">     skippedTests:\n" +
    ">       - TestThatDoesNotWork\n" +
    ">     needsSplit: true\n" +
    "You can extend the functionality of the system by extending the BuildYaml class and adding new elements." +
    "If you do this, be sure to set dtest.core.buildyaml.impl in your properties file to the name of your" +
    "new class.\n" +
    "The following list describes all valid entries in the build.yaml file")
public class BuildYaml {

  /**
   * Implementation of BuildYaml to use to interpret build.yaml.  Defaults to BuildYaml.
   */
  public static final String CFG_BUILDYAML_IMPL = "dtest.core.buildyaml.impl";
  private static final Class<? extends BuildYaml> CFG_BUILDYAML_IMPL_DEFAULT = BuildYaml.class;

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private String baseImage;
  private String[] requiredPackages;
  private String projectName;
  private ModuleDirectory[] dirs;
  private String comment;
  private String[] javaPackages;
  private String[] additionalLogs;

  static Class<? extends BuildYaml> getBuildYamlClass(Config cfg) throws IOException {
    return cfg.getAsClass(CFG_BUILDYAML_IMPL, BuildYaml.class, CFG_BUILDYAML_IMPL_DEFAULT);
  }

  @DocumentEntry(documentName = "yamlfile", documentSection = "elementlist", text="- baseImage: Base docker image" +
      "to use.  Currently supported values are centos, ubuntu, or debian.  These can also include a version" +
      "number if desired.")
  public String getBaseImage() {
    return baseImage;
  }

  public void setBaseImage(String baseImage) {
    this.baseImage = baseImage;
  }

  @DocumentEntry(documentName = "yamlfile", documentSection = "elementlist", text="- requiredPackages: A list of" +
      "required packages that should be installed for your build to work.")
  public String[] getRequiredPackages() {
    return requiredPackages == null ? EMPTY_STRING_ARRAY : requiredPackages;
  }

  public void setRequiredPackages(String[] requiredPackages) {
    this.requiredPackages = requiredPackages;
  }

  @DocumentEntry(documentName = "yamlfile", documentSection = "elementlist", text="- projectName: Name of the project.  " +
      "When using git, this needs to match the directory name of your project when the project is cloned.")
  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  // TODO figure out how to put a link the text here to ModuleDirectory
  @DocumentEntry(documentName = "yamlfile", documentSection = "elementlist", text="- dirs: A list of test groups to" +
      "run.")
  public ModuleDirectory[] getDirs() {
    return dirs;
  }

  public void setDirs(ModuleDirectory[] dirs) {
    this.dirs = dirs;
  }

  @DocumentEntry(documentName = "yamlfile", documentSection = "elementlist", text="- comment: Freeform, all for you" +
      "to comment as you please.")
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @DocumentEntry(documentName = "yamlfile", documentSection = "elementlist", text="- javaPackages: A list of top level" +
      "Java packages that the tests are in.  These are not the individual modules but top level ones, such as" +
      "org.apache.hadoop or org.dtest.  Done as an array because some projects have tests in multiple top level" +
      "packages, e.g. Apache Hive has org.apache.hadoop.hive and org.apache.hive.")
  public String[] getJavaPackages() {
    return javaPackages == null ? EMPTY_STRING_ARRAY : javaPackages;
  }

  public void setJavaPackages(String[] javaPackages) {
    this.javaPackages = javaPackages;
  }

  @DocumentEntry(documentName = "yamlfile", documentSection = "elementlist", text="- additionalLogs: A list of any" +
      "additional log files that should be picked up as part of the collection of log files to ship back to the user.")
  public String[] getAdditionalLogs() {
    return additionalLogs == null ? EMPTY_STRING_ARRAY : additionalLogs;
  }

  public void setAdditionalLogs(String[] additionalLogs) {
    this.additionalLogs = additionalLogs;
  }
}
