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

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import org.dtest.core.impl.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.dtest.core.ContainerCommand.CFG_CONTAINERCOMMAND_TESTSPERCONTAINER;
import static org.dtest.core.ContainerCommand.CFG_CONTAINERCOMMAND_TESTSPERCONTAINER_DEFAULT;

public class BaseContainerCommandList extends ContainerCommandList {
  private static final Logger LOG = LoggerFactory.getLogger(BaseContainerCommandList.class);
  protected int containerNumber;

  public BaseContainerCommandList() {
    containerNumber = 0;

  }

  @Override
  public void buildContainerCommands(ContainerClient containerClient, BuildInfo buildInfo, DTestLogger logger)
      throws IOException {
    setup(containerClient, buildInfo, logger);

    List<BaseModuleDirectory> mDirs = readYaml(buildInfo.getConfDir(), getModuleDirectoryClass());
    for (BaseModuleDirectory mDir : mDirs) {
      mDir.validate();
      int testsPerContainer = mDir.isSetTestsPerContainer() ?
          mDir.getTestsPerContainer() : getConfig().getAsInt(CFG_CONTAINERCOMMAND_TESTSPERCONTAINER,
          CFG_CONTAINERCOMMAND_TESTSPERCONTAINER_DEFAULT);
      if (subclassShouldHandle(mDir)) {
        handle(mDir, containerClient, buildInfo, logger, testsPerContainer);
      } else if (!mDir.getNeedsSplit() && !mDir.isSetSingleTest()) {
        // This is the simple case.  Remove any skipped tests and set any environment variables
        // and we're good
        BaseContainerCommand mvn = new BaseContainerCommand(containerClient.getContainerBaseDir()
            + "/" + mDir.getDir(), containerNumber++);
        setEnvsAndProperties(mDir, mvn);
        if (mDir.isSetSkippedTests()) mvn.excludeTests(mDir.getSkippedTests());
        cmds.add(mvn);
      } else if (mDir.getNeedsSplit()) {
        // Tests that need split
        Set<String> excludedTests = new HashSet<>();
        if (mDir.isSetSkippedTests()) Collections.addAll(excludedTests, mDir.getSkippedTests());
        String unitTests = runContainer(containerClient, mDir.getDir(), buildInfo.getLabel(),
            "find-tests-" + containerNumber++, "find . -name Test\\*\\*.java", logger);
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
            BaseContainerCommand mvn =
                new BaseContainerCommand(containerClient.getContainerBaseDir() + "/" +
                    mDir.getDir(), containerNumber++);
            setEnvsAndProperties(mDir, mvn);
            mvn.addTest(test);
            LOG.debug("Isolating test " + test + " in container " + (containerNumber - 1));
            cmds.add(mvn);
            tests.remove(test);
          }
        }

        while (!tests.isEmpty()) {
          BaseContainerCommand mvn =
              new BaseContainerCommand(containerClient.getContainerBaseDir() + "/" +
              mDir.getDir(), containerNumber++);
          setEnvsAndProperties(mDir, mvn);
          for (int i = 0; i < testsPerContainer && !tests.isEmpty(); i++) {
            String single = tests.pop();
            LOG.debug("Adding test " + single + " to container " + (containerNumber - 1));
            mvn.addTest(single);
          }
          cmds.add(mvn);
        }
      } else if (mDir.isSetSingleTest()) {
        // Running a single test
        BaseContainerCommand mvn =
            new BaseContainerCommand(containerClient.getContainerBaseDir() + "/" + mDir.getDir(),
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

  /**
   * A chance for subclasses to do any setup they need.
   * @param containerClient container client handle.
   * @param buildInfo build information.
   * @param logger dtest logger.
   * @throws IOException
   */
  protected void setup(ContainerClient containerClient, BuildInfo buildInfo, DTestLogger logger)
      throws IOException {

  }

  /**
   * Check if a subclass should handle this instead of the main class.  If this is true then
   * @link {@link #handle(BaseModuleDirectory, ContainerClient, BuildInfo, DTestLogger, int)}
   * will be called.
   * @param mDir information on this directory
   * @return true if the subclass should handle it.
   */
  protected boolean subclassShouldHandle(BaseModuleDirectory mDir) {
    return false;
  }

  /**
   * A chance for subclasses to handle a situation that this class didn't understand.  Default
   * implementation returns false.
   * @param mDir information on this directory
   * @param containerClient container client handle
   * @param buildInfo build information
   * @param logger dtest logger to write details to
   * @param testsPerContainer number tests to run in this container
   * @throws IOException if it fails to read a file or something else it needs
   */
  protected void handle(BaseModuleDirectory mDir, ContainerClient containerClient,
                        BuildInfo buildInfo, DTestLogger logger, int testsPerContainer) throws IOException {
  }

  /**
   * A chance for the subclass to change the ModuleDirectory class used parse the yaml.
   * @return class
   */
  protected Class<? extends BaseModuleDirectory> getModuleDirectoryClass() {
    return BaseModuleDirectory.class;
  }

  @VisibleForTesting
  public <T> List<T> readYaml(String confDir, Class<? extends BaseModuleDirectory> clazz)
      throws IOException {
    String filename = confDir + File.separator + "build.yaml";
    File yamlFile = new File(filename);
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    ObjectReader reader = mapper.readerFor(clazz);
    MappingIterator<T> iter = reader.readValues(yamlFile);
    return iter.readAll();
  }

  protected void setEnvsAndProperties(BaseModuleDirectory mDir, BaseContainerCommand mvn) {
    if (mDir.getEnv() != null) mvn.addEnvs(mDir.getEnv());
    if (mDir.getMvnProperties() != null) mvn.addProperties(mDir.getMvnProperties());
    mvn.setConfig(getConfig());
  }

  protected String runContainer(ContainerClient containerClient, final String dir,
                                final String label, final String containerName,
                                final String cmd, DTestLogger logger) throws IOException {
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
        }, logger);
    if (result.getRc() != 0) {
      String msg = "Failed to run cmd " + cmd + " as part of determining tests to run";
      LOG.error(msg);
      throw new IOException(msg);
    }
    containerClient.removeContainer(result, logger);
    return result.getLogs();
  }
}
