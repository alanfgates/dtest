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

/**
 * BuildYaml is the top level file for holding the object described in the dtest.yaml file.  It contains general
 * build information as well as an array of ModuleDirectory.  Implementations can override this class in order to
 * get information specific to their build.  They can add top level information or subclass
 * {@link ModuleDirectory} in order to add additional information in each module.  If an implementation subclasses
 * ModuleDirectory it will also need to subclass {@link org.dtest.core.mvn.MavenContainerCommandFactory} and
 * implement {@link org.dtest.core.mvn.MavenContainerCommandFactory#getModuleDirs(BuildYaml)}.
 */
public class BuildYaml {

  /**
   * Implementation of `BuildYaml` to use to interpret dtest.yaml.  Defaults to BuildYaml.
   */
  public static final String CFG_BUILDYAML_IMPL = "dtest.core.buildyaml.impl";
  private static final Class<? extends BuildYaml> CFG_BUILDYAML_IMPL_DEFAULT = BuildYaml.class;

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private String baseImage;

  private String[] requiredPackages;

  private String projectName;

  private String projectDir;

  private String repo;

  private String branch;

  private ModuleDirectory[] dirs;

  private String comment;

  private String[] javaPackages;

  private String[] additionalLogs;

  /**
   * Read the YAML file.  If you have configured another class to override BuildYaml it will be used here,
   * otherwise an instance of BuildYaml will be returned.
   * @param confDir Configuration directory, dtest expects the yaml file to be in this directory.
   * @param cfg Configuration object.
   * @param log logger
   * @param repo repository value that should override repo in the yaml file.  This can be left null.
   * @param profile The profile we are building.  The YAML file should be named <i>profile</i>.yaml
   * @param branch branch value that should override branch in the yaml file.  This can be left null.
   * @return yaml as an object.
   * @throws IOException if the file cannot be read.
   */
  public static BuildYaml readYaml(File confDir, Config cfg, DTestLogger log, String repo, String profile, String branch) throws IOException {
    Class<? extends BuildYaml> yamlClass = cfg.getAsClass(CFG_BUILDYAML_IMPL, BuildYaml.class, CFG_BUILDYAML_IMPL_DEFAULT);
    File yamlFile = new File(confDir, profile + ".yaml");
    log.debug("Reading YAML file " + yamlFile.getAbsolutePath() + " and interpreting using " + yamlClass.getName());
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
