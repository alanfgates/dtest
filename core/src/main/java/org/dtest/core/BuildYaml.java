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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

/*~~
 * @document yamlfile
 * @section header
 * # YAML Build File
 * The `dtest.yaml` file determines how the project is built.  It contains information that defines how to
 * build the image (such as the base image and required packages that must be installed) as well as
 * how to run each set of tests.
 *
 * ### Example `dtest.yaml`
 * ```
 * baseImage: centos
 * requiredPackages:
 *   - java-1.8.0-openjdk-devel
 * projectName: myproject
 * javaPackages:
 *   - org.myproject
 * dirs:
 *   - dir: core
 *   - dir: apps
 *     skippedTests:
 *       - TestThatDoesNotWork
 *     needsSplit: true
 * ```
 * You can extend the functionality of the system by extending the `BuildYaml` class and adding new elements.
 * If you do this, be sure to set `dtest.core.buildyaml.impl` in your properties file to the name of your
 * new class.  Doing this allows the implementation to define its own values to be placed in `dtest.yaml`.  To
 * make use of this you will also need to subclass `MavenContainerCommandFactory` so that the custom values are used
 * when generating test commands.
 *
 * The following list describes all valid entries in `dtest.yaml`.
 */
/**
 * BuildYaml is the top level file for holding the object described in the dtest.yaml file.  It contains general
 * build information as well as an array of ModuleDirectory.  Implementations can override this class in order to
 * get information specific to their build.  They can add top level information or subclass
 * {@link ModuleDirectory} in order to add additional information in each module.  If an implementation subclasses
 * ModuleDirectory it will also need to subclass {@link org.dtest.core.mvn.MavenContainerCommandFactory} and
 * implement {@link org.dtest.core.mvn.MavenContainerCommandFactory#getModuleDirs(BuildYaml)}.
 */
public class BuildYaml {

  /*~~
   * @document propsfile
   * @section buildyaml_impl
   * @after buildinfo_label
   * - dtest.core.buildyaml.impl: Subclass of BuildYaml to use to interpret the `dtest.yaml` file.  Defaults
   * to `BuildYaml`.
   */
  /**
   * Implementation of `BuildYaml` to use to interpret dtest.yaml.  Defaults to BuildYaml.
   */
  public static final String CFG_BUILDYAML_IMPL = "dtest.core.buildyaml.impl";
  private static final Class<? extends BuildYaml> CFG_BUILDYAML_IMPL_DEFAULT = BuildYaml.class;

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  /*~~
   * @document yamlfile
   * @section el_i1
   * @after header
   * - baseImage: Base docker image to use.  Currently supported values are `centos`, `ubuntu`, or `debian`.  These
   * can also include a version number if desired.
   */
  private String baseImage;

  /*~~
   * @document yamlfile
   * @section el_i2
   * @after el_i1
   * - requiredPackages: A list of required packages that should be installed for your build to work.
   */
  private String[] requiredPackages;

  /*~~
   * @document yamlfile
   * @section projectname
   * @after el_i2
   * - projectName: Name of the project.  When using git, this needs to match the directory name of your project
   * when the project is cloned.
   */
  private String projectName;

  /*~~
   * @document yamlfile
   * @section projectdir
   * @after projectname
   * - projectDir: Directory in the project to run `mvn install` in when building the image.  Usually this will be
   * the same as `projectName`.  In that case this value does not need to be set, and the value of `projectName`
   * will be used.
   */
  private String projectDir;

  /*~~
   * @document yamlfile
   * @section repo
   * @after projectdir
   * - repo: Default repository to use for source control.  This can be overridden by values passed on
   * the command line or to the plugin.
   */
  private String repo;

  /*~~
   * @document yamlfile
   * @section branch
   * @after repo
   * - branch: Default branch to use with source control.  This can be overridden by values passed on the
   * command line or to the plugin.  The default for this value is source control specific.  For git it is 'master'.
   */
  private String branch;

  /*~~
   * @document yamlfile
   * @section el_i4
   * @after branch
   * - dirs: A list of test groups to run.  Each element of this list is a `ModuleDirectory`.
   */
  private ModuleDirectory[] dirs;

  /*~~
   * @document yamlfile
   * @section el_i5
   * @after el_i4
   * - comment: Free form, all for you to comment as you please.
   */
  private String comment;

  /*~~
   * @document yamlfile
   * @section el_i6
   * @after el_i5
   * - javaPackages: A list of top level Java packages that the tests are in.  These are not the individual
   * modules but top level ones, such as org.apache.hadoop or org.dtest.  Done as a list because some projects
   * have tests in multiple top level packages, e.g. Apache Hive has tests in org.apache.hadoop.hive and org.apache.hive.
   */
  private String[] javaPackages;

  /*~~
   * @document yamlfile
   * @section el_i7
   * @after el_i6
   * - additionalLogs: A list of any additional log files that should be picked up as part of the collection of
   * log files to ship back to the user.  By default the system picks up output from the `surefire-reports`
   * directory.  If your system uses log4j or a similar package and you want to fetch the resulting logs you should
   * place that log in this list as dtest cannot determine how the logging is configured and where the logfile is.
   *
   */
  private String[] additionalLogs;

  /**
   * Read the YAML file.  If you have configured another class to override BuildYaml it will be used here,
   * otherwise an instance of BuildYaml will be returned.
   * @param confDir Configuration directory, dtest expects dtest.yaml to be in this directory.
   * @param cfg Configuration object.
   * @param log logger
   * @param repo repository value that should override repo in the yaml file.  This can be left null.
   * @param branch branch value that should override branch in the yaml file.  This can be left null.
   * @return yaml as an object.
   * @throws IOException if the file cannot be read.
   */
  public static BuildYaml readYaml(String confDir, Config cfg, DTestLogger log, String repo, String branch) throws IOException {
    String filename = confDir + File.separator + Config.YAML_FILE;
    Class<? extends BuildYaml> yamlClass = cfg.getAsClass(CFG_BUILDYAML_IMPL, BuildYaml.class, CFG_BUILDYAML_IMPL_DEFAULT);
    log.debug("Reading YAML file " + filename + " and interpreting using " + yamlClass.getName());
    File yamlFile = new File(filename);
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    ObjectReader reader = mapper.readerFor(yamlClass);
    BuildYaml yaml = reader.readValue(yamlFile);
    if (repo != null) yaml.setRepo(repo);
    if (branch != null) yaml.setBranch(branch);
    return yaml;
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

  public String getProjectDir() {
    return projectDir == null ? projectName : projectDir;
  }

  public void setProjectDir(String projectDir) {
    this.projectDir = projectDir;
  }

  public String getRepo() {
    return repo;
  }

  public void setRepo(String repo) {
    this.repo = repo;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
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
