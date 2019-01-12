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
package org.dtest.hive;

import org.apache.commons.lang3.StringUtils;
import org.dtest.core.BuildInfo;
import org.dtest.core.BuildYaml;
import org.dtest.core.ContainerClient;
import org.dtest.core.ContainerCommand;
import org.dtest.core.mvn.MavenContainerCommandFactory;
import org.dtest.core.ModuleDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class HiveContainerCommandFactory extends MavenContainerCommandFactory {
  private static final Logger LOG = LoggerFactory.getLogger(HiveContainerCommandFactory.class);
  private Properties testProperties;
  private Map<String, Set<String>> filesFromDirs = new HashMap<>();

  @Override
  public void setup(ContainerClient containerClient, BuildInfo buildInfo)
      throws IOException {
    // Read the test properties file as a number of things need info in there
    String testPropertiesString = runContainer(containerClient, ".", buildInfo.getLabel(), "read-testconfiguration",
        "cat itests/src/test/resources/testconfiguration.properties");
    testProperties = new Properties();
    testProperties.load(new StringReader(testPropertiesString));
  }

  @Override
  public List<String> getInitialBuildCommand() {
    return Arrays.asList(
      "/usr/bin/mvn install -DskipTests",
      "cd itests",
      "/usr/bin/mvn install -DskipSparkTests -DskipTests");
  }

  @Override
  protected void buildOneContainerCommand(ModuleDirectory simple, ContainerClient containerClient,
                                          BuildInfo buildInfo, int testsPerContainer) throws IOException {
    assert simple instanceof HiveModuleDirectory;
    HiveModuleDirectory mDir = (HiveModuleDirectory)simple;
    if (mDir.isSetSingleTest() && mDir.hasQFiles()) {
      // We only need to handle this if it's working with qtest.  Otherwise, pass it back to our parent.
      Set<String> qfiles;
      if (mDir.isSetQFilesDir() || mDir.isSetIncludedQFilesProperties()) {
        // If we're supposed to read the qfiles from a directory and/or properties, do that
        qfiles = findQFiles(containerClient, buildInfo.getLabel(), mDir);
      } else {
        // Or if we've been given a list of qfiles, use that
        qfiles = new HashSet<>();
        Collections.addAll(qfiles, mDir.getQFiles());
      }
      // Deal with any tests that need to be run alone
      if (mDir.isSetIsolatedQFiles()) {
        for (String test : mDir.getIsolatedQFiles()) {
          cmds.add(buildOneQFilesCmd(containerClient, Collections.singleton(test), mDir));
          qfiles.remove(test);
        }
      }

      while (!qfiles.isEmpty()) {
        List<String> oneSet = new ArrayList<>(testsPerContainer);
        for (String qFile : qfiles) {
          if (oneSet.size() >= testsPerContainer) break;
          oneSet.add(qFile);
        }
        cmds.add(buildOneQFilesCmd(containerClient, oneSet, mDir));
        qfiles.removeAll(oneSet);
      }
    } else {
      super.buildOneContainerCommand(simple, containerClient, buildInfo, testsPerContainer);
    }
  }

  @Override
  protected ModuleDirectory[] getModuleDirs(BuildYaml yaml) {
    log.debug("HiveContainerCommandFactory fetching module directories");
    assert yaml instanceof HiveBuildYaml;
    return ((HiveBuildYaml)yaml).getHiveDirs();
  }

  private ContainerCommand buildOneQFilesCmd(ContainerClient containerClient,
                                             Collection<String> qfiles,
                                             HiveModuleDirectory mDir) {
    HiveContainerCommand mvn = new HiveContainerCommand(containerClient.getContainerBaseDir() + "/" + mDir.getDir(),
        containerNumber++);
    setEnvsAndProperties(mDir, mvn);
    mvn.addTest(mDir.getSingleTest());
    for (String qfile : qfiles) {
      LOG.debug("Adding qfile " + qfile + " to container " + (containerNumber - 1));
      mvn.addQfile(qfile);
    }
    return mvn;
  }

  private Set<String> findQFilesFromProperties(String... properties) {
    Set<String> qfiles = new HashSet<>();
    for (String property : properties) {
      if (testProperties.getProperty(property) != null) {
        Collections.addAll(qfiles, testProperties.getProperty(property).split(","));
      }
    }
    return qfiles;
  }

  private Set<String> findQFiles(ContainerClient containerClient, String label, HiveModuleDirectory mDir) {
    // Find all of the qfile tests.  The logic here is that if a specific set of included files
    // have been listed, then use those.  Otherwise read all the files from the indicated
    // directory.  In either case apply any excludes from properties or specifically excluded files.
    Set<String> qfiles;

    if (mDir.isSetIncludedQFilesProperties()) {
      String[] includedProps = mDir.getIncludedQFilesProperties();
      log.debug("For test " + mDir.getSingleTest() + " found included properties " + StringUtils.join(includedProps, " "));
      qfiles = findQFilesFromProperties(mDir.getIncludedQFilesProperties());
      log.debug("For test " + mDir.getSingleTest() + " resolved included properties to following qfiles " +
          StringUtils.join(qfiles, " "));
    } else {
      qfiles = filesFromDirs.computeIfAbsent(mDir.getQFilesDir(), s -> {
        try {
          String allQFiles = runContainer(containerClient, mDir.getQFilesDir(), label,
              "qfile-finder-" + containerNumber++, "find . -name \\*.q -maxdepth 1");


          Set<String> runnableQfiles = new HashSet<>();
          for (String line : allQFiles.split("\n")) {
            String testPath = line.trim();
            String[] pathElements = testPath.split(File.separator);
            String testName = pathElements[pathElements.length - 1];
            runnableQfiles.add(testName);
          }
          return runnableQfiles;
        } catch (IOException e) {
          LOG.error("Unable to find files for directory " + mDir.getQFilesDir(), e);
          throw new RuntimeException(e);
        }
      });
    }
    if (mDir.isSetExcludedQFilesProperties()) {
      String[] excludedProps = mDir.getExcludedQFilesProperties();
      log.debug("For test " + mDir.getSingleTest() + " found excluded properties " + StringUtils.join(excludedProps, " "));
      Set<String> toRemove = findQFilesFromProperties(mDir.getExcludedQFilesProperties());
      log.debug("For test " + mDir.getSingleTest() + " found following qfiles to exclude based on properties " +
          StringUtils.join(toRemove, " "));
      qfiles.removeAll(toRemove);
    }
    if (mDir.isSetExcludedQFiles()) {
      log.debug("For test " + mDir.getSingleTest() + " removing following qfiles as they are marked excluded: " +
          StringUtils.join(mDir.getExcludedQFiles(), " "));
      qfiles.removeAll(Arrays.asList(mDir.getExcludedQFiles()));
    }
    log.debug("findQFiles returning following qfiles for test " + mDir.getSingleTest() + ": " +
        StringUtils.join(qfiles, " "));
    return qfiles;
  }
}
