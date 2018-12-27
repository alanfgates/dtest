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
package org.dtest.maven;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.dtest.core.BuildInfo;
import org.dtest.core.BuildState;
import org.dtest.core.Config;
import org.dtest.core.DTestLogger;
import org.dtest.core.DockerTest;
import org.dtest.core.git.GitSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * A maven plugin for dtest.  It is not suggested that you set this up to run by default, as you are probably only
 * using dtest if you have a complex and long build process.  This plugin can be invoked by doing 'mvn dtest:dtest'.
 * It is built as an aggregator plugin so it will only run at the top level of your build, not in each module.
 */
@Mojo(name = "dtest",
      aggregator = true,
      defaultPhase = LifecyclePhase.VERIFY)
public class DtestPlugin extends AbstractMojo {

  /**
   * Default source control repository to use in the build.  Normally you would set this in your dtest.properties
   * file, but you can override it here if needed.
   */
  @Parameter(property = "dtest.repo")
  private String repo;

  /**
   * Branch to be used in the source control.  Defaults to something reasonable for the configured source
   * control system.
   */
  @Parameter(property = "dtest.branch")
  private String branch;

  /**
   * Label to use with the build.  Usually you don't want to set this as the system will generate a unique
   * label for this build to make sure it is built from scratch.  Only set this if you explicitly want to rerun
   * an existing build.
   */
  @Parameter(property = "dtest.label")
  private String label;

  /**
   * Base directory to run the build in.  Defaults to target/dtest-plugin-build
   */
  @Parameter(property = "dtest.basedir", defaultValue = "${project.build.directory}/dtest-plugin-build")
  private File baseDir;

  /**
   * Where the plugin will look for the dtest.properties and dtest.yaml files.  Defaults to src/test/resources.
   */
  @Parameter(defaultValue = "${project.build.testResources}")
  private List<Resource> testResources;

  /**
   * Additional properties to set for the build.  These will be used in constructing the configuration object
   * for the build.  They will override any values in the build's config file.
   */
  @Parameter(property = "dtest.properties")
  private Properties dtestProperties;

  /**
   * Whether to cleanup after the build.  Defaults to true.  If set to false the docker image and containers
   * used in the build will be left around.  Useful for debugging.
   */
  @Parameter(property = "dtest.cleanupafter", defaultValue = "true")
  private boolean cleanupAfter;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    DTestLogger log = new MavenLogger(getLog());

    if (dtestProperties == null) dtestProperties = new Properties();

    if (repo != null) {
      dtestProperties.setProperty(GitSource.CFG_CODESOURCE_REPO, repo);
      log.debug("Building with repo set to " + repo);
    }

    if (branch != null) {
      dtestProperties.setProperty(GitSource.CFG_CODESOURCE_BRANCH, branch);
      log.debug("Building with branch set to " + branch);
    }

    if (label == null) {
      label = RandomStringUtils.randomAlphanumeric(10).toLowerCase();
    }
    dtestProperties.setProperty(BuildInfo.CFG_BUILDINFO_LABEL, label);
    log.info("Building with label " + label);

    baseDir.mkdirs();
    log.debug("Building in " + baseDir.getAbsolutePath());
    dtestProperties.setProperty(BuildInfo.CFG_BUILDINFO_BASEDIR, baseDir.getAbsolutePath());

    for (Resource r : testResources) {
      log.warn("targetPath: " + r.getTargetPath() + " dir: " + r.getDirectory() + " includes: " +
          StringUtils.join(r.getIncludes(), " "));
    }

    File propertiesFile = null, yamlFile = null;
    for (Resource resource : testResources) {
      File maybeProperties = new File(resource.getDirectory(), Config.PROPERTIES_FILE);
      if (maybeProperties.exists()) propertiesFile = maybeProperties;
      File maybeYaml = new File(resource.getDirectory(), Config.YAML_FILE);
      if (maybeYaml.exists()) yamlFile = maybeYaml;
    }
    if (propertiesFile == null || yamlFile == null) {
      StringBuilder buf = new StringBuilder("Unable to find properties file and/or yaml file for this build, looked in ");
      testResources.forEach(r -> buf.append(r.getDirectory()));
      throw new MojoFailureException(buf.toString());
    }
    copyFile(propertiesFile, baseDir);
    copyFile(yamlFile, baseDir);

    log.debug("Building with properties " + dtestProperties.toString());

    DockerTest dtest = new DockerTest();
    try {
      dtest.buildConfig(baseDir.getAbsolutePath(), dtestProperties, cleanupAfter);
    } catch (IOException e) {
      throw new MojoFailureException("Failed to build the configuration for the build", e);
    }
    dtest.setLogger(log);
    BuildState state = dtest.runBuild();
    switch (state.getState()) {
      case HAD_FAILURES_OR_ERRORS: throw new MojoFailureException("Build had tests that failed or returned an error.");
      case HAD_TIMEOUTS: throw new MojoFailureException("Build had tests that timed out.");
      case FAILED: throw new MojoFailureException("Build failed");
      case TIMED_OUT: throw new MojoFailureException("Build timed out");
      case SUCCEEDED: break;
      default: throw new MojoExecutionException("Unknown build state.");
    }
    log.info("Build succeeded.");
  }

  private void copyFile(File src, File dstDir) throws MojoFailureException {
    getLog().debug("Going to copy file " + src.getAbsolutePath() + " to " + dstDir.getAbsolutePath());
    try {
      BufferedReader reader = new BufferedReader(new FileReader(src));
      File dstFile = new File(dstDir, src.getName());
      FileWriter writer = new FileWriter(dstFile);
      String line;
      while ((line = reader.readLine()) != null) writer.write(line + "\n");
      reader.close();
      writer.close();
    } catch (IOException e) {
      throw new MojoFailureException("Unable to copy " + src.getAbsolutePath() + " to " + dstDir.getAbsolutePath(), e);
    }

  }



}
