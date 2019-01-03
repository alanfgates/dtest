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

/*~~
 * @document plugin
 * @section intro
 * # Dtest Maven Plugin
 *
 * A plugin to support running dtest as part of your build and test process. It is not suggested that this be set up
 * to run by default, as test is probably only being used for a project with a long and complex build process.
 * This plugin can be invoked by doing `mvn dtest:dtest`.
 *
 * It is built as an aggregator plugin so it will only run at the top level of a build, not in each module.
 *
 * The following values can be set in your pom to configure dtest:
 */
/*~~
 * @document plugin
 * @section examples
 * @after cleanupafter
 *
 * ## Examples
 * To use the plugin with no additional configuration:
 * ```
 * <build>
 *   <plugins>
 *     <plugin>
 *       <groupId>org.dtest</groupId>
 *       <artifactId>dtest-maven-plugin</artifactId>
 *       <version>0.2-SNAPSHOT</version>
 *     </plugin>
 *   </plugins>
 * </build>
 * ```
 *
 * This will not automatically run the plugin in any phase.  That is not recommended behavior.  The plugin can be
 * manually invoked using `mvn dtest:dtest`.
 *
 * To set the repository for your code in the pom:
 * ```
 * <build>
 *   <plugins>
 *     <plugin>
 *       <groupId>org.dtest</groupId>
 *       <artifactId>dtest-maven-plugin</artifactId>
 *       <version>0.2-SNAPSHOT</version>
 *       <configuration>
 *         <repo>https://github.com/mygithub/myproject.git</repo>
 *       </configuration>
 *     </plugin>
 *   </plugins>
 * </build>
 * ```
 */
/**
 * A maven plugin for dtest.
 */
@Mojo(name = "dtest",
      aggregator = true,
      defaultPhase = LifecyclePhase.VERIFY)
public class DtestPlugin extends AbstractMojo {

  /*~~
   * @document plugin
   * @section repo
   * @after intro
   * - repo:  Default source control repository to use in the build.  This can be set in the `dtest.properties`
   * file or in the pom.  It can also be set via the property `dtest.repo` when invoking maven.
   */
  @Parameter(property = "dtest.repo")
  private String repo;

  /*~~
   * @document plugin
   * @section branch
   * @after repo
   * - branch:  Branch to be used in source control.  Defaults to something reasonable for the configured source
   * control system, e.g. master for git. It can also be set via the property `dtest.branch` when invoking maven.
   */
  @Parameter(property = "dtest.branch")
  private String branch;

  /*~~
   * @document plugin
   * @section label
   * @after branch
   * - label: Label to use with the build.  Usually this should not be set as the system will generate a unique
   * label for this build to assure it is built from scratch.  If an existing build image should be reused this
   * value can be set to the associated label.  It can also be set via the property `dtest.label` when invoking maven.
   */
  @Parameter(property = "dtest.label")
  private String label;

  /*~~
   * @document plugin
   * @section basedir
   * @after label
   * - baseDir: Working directory for the build on the build machine.  Defaults to
   * `${project.build.directory}/dtest-plugin-build` (`target/dtest-plugin-build` by default).  It can also be set via
   * the property `dtest.basedir` when invoking maven.
   */
  @Parameter(property = "dtest.basedir", defaultValue = "${project.build.directory}/dtest-plugin-build")
  private File baseDir;

  /*~~
   * @document plugin
   * @section testresources
   * @after basedir
   * - testResources: Directory that the plugin will look for the `dtest.properties` and `dtest.yaml` files.
   * Defaults to `${project.build.testResources}` which usually points to `src/test/resources`.
   */
  @Parameter(defaultValue = "${project.build.testResources}")
  private List<Resource> testResources;

  /*~~
   * @document plugin
   * @section properties
   * @after testresources
   * - dtestProperties: Additional properties to set for the build.  These will be used in constructing the configuration object
   * for the build.  They will override any values in the `dtest.properties` config file.
   */
  @Parameter
  private Properties dtestProperties;

  /*~~
   * @document plugin
   * @section cleanupafter
   * @after properties
   * - cleanupAfter: Whether to remove the docker containers and image after the build.  Defaults to true.  Useful for debugging.
   * If this is set to false always a large number of images and containers will pollute the build machine.
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
      log.info("targetPath: " + r.getTargetPath() + " dir: " + r.getDirectory() + " includes: " +
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
