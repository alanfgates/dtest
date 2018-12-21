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

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import org.dtest.core.BuildInfo;
import org.dtest.core.ContainerClient;
import org.dtest.core.ContainerCommand;
import org.dtest.core.ContainerCommandFactory;
import org.dtest.core.ContainerResult;
import org.dtest.core.impl.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MavenContainerCommandFactory extends ContainerCommandFactory {

  public static final String YAML_FILE = "dtest.yaml";

  protected int containerNumber;

  public MavenContainerCommandFactory() {
    containerNumber = 0;

  }

  @Override
  public void buildContainerCommands(ContainerClient containerClient, BuildInfo buildInfo)
      throws IOException {
    setup(containerClient, buildInfo);

    List<ModuleDirectory> mDirs = readYaml(buildInfo.getConfDir(), getModuleDirectoryClass());
    for (ModuleDirectory mDir : mDirs) {
      mDir.validate();
      int testsPerContainer = mDir.isSetTestsPerContainer() ?
          mDir.getTestsPerContainer() : cfg.getAsInt(CFG_CONTAINERCOMMANDFACTORY_TESTSPERCONTAINER,
          CFG_CONTAINERCOMMANDFACTORY_TESTSPERCONTAINER_DEFAULT);
      if (subclassShouldHandle(mDir)) {
        handle(mDir, containerClient, buildInfo, testsPerContainer);
      } else if (!mDir.getNeedsSplit() && !mDir.isSetSingleTest()) {
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
   * @throws IOException
   */
  protected void setup(ContainerClient containerClient, BuildInfo buildInfo)
      throws IOException {

  }

  /**
   * Check if a subclass should handle this instead of the main class.  If this is true then
   * @link {@link #handle(ModuleDirectory, ContainerClient, BuildInfo, int)}
   * will be called.
   * @param mDir information on this directory
   * @return true if the subclass should handle it.
   */
  protected boolean subclassShouldHandle(ModuleDirectory mDir) {
    return false;
  }

  /**
   * A chance for subclasses to handle a situation that this class didn't understand.  Default
   * implementation returns false.
   * @param mDir information on this directory
   * @param containerClient container client handle
   * @param buildInfo build information
   * @param testsPerContainer number tests to run in this container
   * @throws IOException if it fails to read a file or something else it needs
   */
  protected void handle(ModuleDirectory mDir, ContainerClient containerClient,
                        BuildInfo buildInfo, int testsPerContainer) throws IOException {
  }

  /**
   * A chance for the subclass to change the ModuleDirectory class used parse the yaml.
   * @return class
   */
  protected Class<? extends ModuleDirectory> getModuleDirectoryClass() {
    return ModuleDirectory.class;
  }

  @VisibleForTesting
  public <T> List<T> readYaml(String confDir, Class<? extends ModuleDirectory> clazz)
      throws IOException {
    String filename = confDir + File.separator + YAML_FILE;
    File yamlFile = new File(filename);
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    ObjectReader reader = mapper.readerFor(clazz);
    MappingIterator<T> iter = reader.readValues(yamlFile);
    return iter.readAll();
  }

  protected void setEnvsAndProperties(ModuleDirectory mDir, MavenContainerCommand mvn) {
    if (mDir.getEnv() != null) mvn.addEnvs(mDir.getEnv());
    if (mDir.getMvnProperties() != null) mvn.addProperties(mDir.getMvnProperties());
    mvn.setConfig(cfg).setLog(log);
  }

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
    return result.getLogs();
  }
}
