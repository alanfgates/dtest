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

import org.dtest.core.BuildInfo;
import org.dtest.core.ContainerClient;
import org.dtest.core.ContainerCommand;
import org.dtest.core.DTestLogger;
import org.dtest.core.simple.SimpleContainerCommandFactory;
import org.dtest.core.simple.SimpleModuleDirectory;
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

public class HiveContainerCommandFactory extends SimpleContainerCommandFactory {
  private static final Logger LOG = LoggerFactory.getLogger(HiveContainerCommandFactory.class);
  private Properties testProperties;
  private Map<String, Set<String>> filesFromDirs = new HashMap<>();

  @Override
  public void setup(ContainerClient containerClient, BuildInfo buildInfo, DTestLogger logger)
      throws IOException {
    // Read the test properties file as a number of things need info in there
    String testPropertiesString = runContainer(containerClient, ".", buildInfo.getLabel(), "read-testconfiguration",
        "cat itests/src/test/resources/testconfiguration.properties", logger);
    testProperties = new Properties();
    testProperties.load(new StringReader(testPropertiesString));
  }

  @Override
  protected boolean subclassShouldHandle(SimpleModuleDirectory simple) {
    assert simple instanceof HiveModuleDirectory;
    HiveModuleDirectory mDir = (HiveModuleDirectory)simple;
    return mDir.isSetSingleTest() && mDir.hasQFiles();
  }

  @Override
  protected void handle(SimpleModuleDirectory simple, ContainerClient containerClient,
                        BuildInfo buildInfo, DTestLogger logger, List<ContainerCommand> cmds,
                        int testsPerContainer) throws IOException {
    assert simple instanceof HiveModuleDirectory;
    HiveModuleDirectory mDir = (HiveModuleDirectory)simple;
    Set<String> qfiles;
    if (mDir.isSetQFilesDir() || mDir.isSetIncludedQFilesProperties()) {
      // If we're supposed to read the qfiles from a directory and/or properties, do that
      qfiles = findQFiles(containerClient, buildInfo.getLabel(), logger, mDir);
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
  }

  @Override
  protected Class<? extends SimpleModuleDirectory> getModuleDirectoryClass() {
    return HiveModuleDirectory.class;
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

  private Set<String> findQFiles(ContainerClient containerClient, String label, DTestLogger logger,
                                 HiveModuleDirectory mDir) throws IOException {
    // Find all of the qfile tests.  The logic here is that if a specific set of included files
    // have been listed, then use those.  Otherwise read all the files from the indicated
    // directory.  In either case apply any excludes from properties or specifically excluded files.
    Set<String> qfiles;

    if (mDir.isSetIncludedQFilesProperties()) {
      qfiles = findQFilesFromProperties(mDir.getIncludedQFilesProperties());
    } else {
      qfiles = filesFromDirs.computeIfAbsent(mDir.getDir(), s -> {
        try {
          String allQFiles = runContainer(containerClient, mDir.getQFilesDir(), label,
              "qfile-finder-" + containerNumber++, "find . -name \\*.q -maxdepth 1", logger);


          Set<String> runnableQfiles = new HashSet<>();
          for (String line : allQFiles.split("\n")) {
            String testPath = line.trim();
            String[] pathElements = testPath.split(File.separator);
            String testName = pathElements[pathElements.length - 1];
            runnableQfiles.add(testName);
          }
          return runnableQfiles;
        } catch (IOException e) {
          LOG.error("Unable to find files for directory " + mDir.getDir(), e);
          throw new RuntimeException(e);
        }
      });
    }
    if (mDir.isSetExcludedQFilesProperties()) {
      qfiles.removeAll(findQFilesFromProperties(mDir.getExcludedQFilesProperties()));
    }
    if (mDir.isSetExcludedQFiles()) {
      qfiles.removeAll(Arrays.asList(mDir.getExcludedQFiles()));
    }
    return qfiles;
  }
}
