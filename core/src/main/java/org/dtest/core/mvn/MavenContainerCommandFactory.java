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
package org.dtest.core.mvn;

import org.dtest.core.BuildInfo;
import org.dtest.core.BuildYaml;
import org.dtest.core.ContainerClient;
import org.dtest.core.ContainerCommand;
import org.dtest.core.ContainerCommandFactory;
import org.dtest.core.ContainerResult;
import org.dtest.core.ModuleDirectory;
import org.dtest.core.impl.Utils;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of ContainerCommandFactory with maven specific logic.
 */
public class MavenContainerCommandFactory extends ContainerCommandFactory {

  /**
   * Next container number to use.  Each container gets a number for use in its name and logging.
   */
  protected int containerNumber;

  public MavenContainerCommandFactory() {
    containerNumber = 0;

  }

  @Override
  public void buildContainerCommands(ContainerClient containerClient, BuildInfo buildInfo)
      throws IOException {
    setup(containerClient, buildInfo);

    ModuleDirectory[] mDirs = getModuleDirs(buildInfo.getYaml());
    for (ModuleDirectory mDir : mDirs) {
      mDir.validate();
      int testsPerContainer = mDir.isSetTestsPerContainer() ?
          mDir.getTestsPerContainer() : cfg.getAsInt(CFG_CONTAINERCOMMANDFACTORY_TESTSPERCONTAINER,
          CFG_CONTAINERCOMMANDFACTORY_TESTSPERCONTAINER_DEFAULT);
      buildOneContainerCommand(mDir, containerClient, buildInfo, testsPerContainer);
    }
  }

  @Override
  public List<String> getInitialBuildCommand() {
    return Collections.singletonList("/usr/bin/mvn install -DskipTests");
  }

  @Override
  public List<String> getRequiredPackages() {
    return Arrays.asList("unzip", "maven");
  }

  /**
   * A chance for subclasses to do any setup they need.
   * @param containerClient container client handle.
   * @param buildInfo build information.
   * @throws IOException if the setup fails
   */
  protected void setup(ContainerClient containerClient, BuildInfo buildInfo)
      throws IOException {

  }

  /**
   * Build an individual container command.  This implementation works with the options present in
   * {@link BuildYaml}.  If a subclass has subclassed BuildYaml and wishes to handle things differently
   * they can override this method.
   * @param mDir information on this directory
   * @param containerClient container client handle
   * @param buildInfo build information
   * @param testsPerContainer number tests to run in this container
   * @throws IOException if it fails to read a file or something else it needs
   */
  protected void buildOneContainerCommand(ModuleDirectory mDir, ContainerClient containerClient,
                                          BuildInfo buildInfo, int testsPerContainer) throws IOException {
    if (!mDir.getNeedsSplit() && !mDir.isSetSingleTest()) {
      // This is the simple case.  Remove any skipped tests and set any environment variables
      // and we're good
      MavenContainerCommand mvn = new MavenContainerCommand(containerClient.getContainerBaseDir()
          + "/" + mDir.getDir(), containerNumber++);
      setEnvsAndProperties(mDir, mvn);
      if (mDir.isSetSkippedTests()) mvn.excludeTests(mDir.getSkippedTests());
      cmds.add(mvn);
    } else if (mDir.getNeedsSplit()) {
      // Tests that need split
      Set<String> excludedTests = new HashSet<>();
      if (mDir.isSetSkippedTests()) Collections.addAll(excludedTests, mDir.getSkippedTests());
      String unitTests = runContainer(containerClient, mDir.getDir(), buildInfo.getLabel(),
          "find-tests-" + containerNumber++, "find . -name Test\\*\\*.java");
      Deque<String> tests = new ArrayDeque<>();
      for (String line : unitTests.split("\n")) {
        String testPath = line.trim();

        // Isolate the test name
        String[] pathElements = testPath.split("/");
        String testName = pathElements[pathElements.length - 1];
        if (testName.endsWith(".java")) testName = testName.substring(0, testName.length() - 5);
        if (!excludedTests.contains(testName)) tests.add(testName);
      }

      // deal with isolated tests
      if (mDir.isSetIsolatedTests()) {
        for (String test : mDir.getIsolatedTests()) {
          MavenContainerCommand mvn =
              new MavenContainerCommand(containerClient.getContainerBaseDir() + "/" +
                  mDir.getDir(), containerNumber++);
          setEnvsAndProperties(mDir, mvn);
          mvn.addTest(test);
          log.debug("Isolating test " + test + " in container " + (containerNumber - 1));
          cmds.add(mvn);
          tests.remove(test);
        }
      }

      while (!tests.isEmpty()) {
        MavenContainerCommand mvn =
            new MavenContainerCommand(containerClient.getContainerBaseDir() + "/" +
                mDir.getDir(), containerNumber++);
        setEnvsAndProperties(mDir, mvn);
        for (int i = 0; i < testsPerContainer && !tests.isEmpty(); i++) {
          String single = tests.pop();
          log.debug("Adding test " + single + " to container " + (containerNumber - 1));
          mvn.addTest(single);
        }
        cmds.add(mvn);
      }
    } else if (mDir.isSetSingleTest()) {
      // Running a single test
      MavenContainerCommand mvn =
          new MavenContainerCommand(containerClient.getContainerBaseDir() + "/" + mDir.getDir(),
              containerNumber++);
      mvn.addTest(mDir.getSingleTest());
      setEnvsAndProperties(mDir, mvn);
      cmds.add(mvn);
    } else {
      throw new InvalidObjectException("Help, I don't understand what you want me to do for " +
          "directory " + mDir.getDir());
    }
  }

  /**
   * A chance for the subclass to override how the module directories are obtained.  This should be used if the
   * subclass has overridden the implementation of BuildYaml and it wants to return module directories with
   * information specific to itself, and thus needs a call other than getDirs from BuildYaml.
   * @param yaml yaml file information
   * @return array of module directories, probably really of some subclass of ModuleDirectory.
   */
  protected ModuleDirectory[] getModuleDirs(BuildYaml yaml) {
    return yaml.getDirs();
  }

  /**
   * Translate environment variable and properties from the yaml file to the container command.  Protected so
   * that subclasses can use it when constructing commands.
   * @param mDir information for this command from the yaml file.
   * @param mvn container command
   */
  protected void setEnvsAndProperties(ModuleDirectory mDir, MavenContainerCommand mvn) {
    if (mDir.getEnv() != null) mvn.addEnvs(mDir.getEnv());
    if (mDir.getProperties() != null) mvn.addProperties(mDir.getProperties());
    mvn.setConfig(cfg).setLog(log);
  }

  /**
   * Run a container as part of building container commands.  This is not intended to run the test commands,
   * {@link MavenContainerCommand} is for that.  This is used to run quick running containers that are used
   * for things like listing the contents of a directory and finding all of the tests.  Protected so that subclasses
   * can use it in constructing their commands.
   * @param containerClient used to run the container
   * @param dir directory in the container in which the command should be run.  This is relative to the container
   *            base directory.
   * @param label label for this build
   * @param containerName name of this particular container, should be unique
   * @param cmd command to run as a single string.  This will be run as a shell command with no escaping etc. done.
   * @return the stdout of the container.
   * @throws IOException if the container fails to run, including if the command returns a non-zero return code.
   */
  protected String runContainer(ContainerClient containerClient, final String dir,
                                final String label, final String containerName,
                                final String cmd) throws IOException {
    ContainerResult result = containerClient.runContainer(
        new ContainerCommand() {
          @Override
          public String containerSuffix() {
            return Utils.buildContainerName(label, containerName);
          }

          @Override
          public String[] shellCommand() {
            return Utils.shellCmdInRoot(
                containerClient.getContainerBaseDir() + (dir == null ? "" : "/" + dir), ()-> cmd);
          }

          @Override
          public String containerDirectory() {
            return dir;
          }
        });
    if (result.getRc() != 0) {
      String msg = "Failed to run cmd " + cmd + " as part of determining tests to run";
      log.error(msg);
      throw new IOException(msg);
    }
    containerClient.removeContainer(result);
    return result.getStdout();
  }
}
