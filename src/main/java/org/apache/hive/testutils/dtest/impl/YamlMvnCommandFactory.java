/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hive.testutils.dtest.impl;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import org.apache.hive.testutils.dtest.BuildInfo;
import org.apache.hive.testutils.dtest.Config;
import org.apache.hive.testutils.dtest.ContainerClient;
import org.apache.hive.testutils.dtest.ContainerCommand;
import org.apache.hive.testutils.dtest.ContainerCommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class YamlMvnCommandFactory extends ContainerCommandFactory {
  private static final Logger LOG = LoggerFactory.getLogger(MvnCommandFactory.class);
  private int containerNumber;
  private Properties testProperties;

  @Override
  public List<ContainerCommand> getContainerCommands(ContainerClient containerClient,
                                                     BuildInfo buildInfo,
                                                     DTestLogger logger) throws IOException {
    containerNumber = 0;

    // Read the test properties file as a number of things need info in there
    String testPropertiesString = runContainer(containerClient, ".", buildInfo.getLabel(), "read-testconfiguration",
        "cat itests/src/test/resources/testconfiguration.properties", logger);
    testProperties = new Properties();
    testProperties.load(new StringReader(testPropertiesString));

    List<ModuleDirectory> mDirs = readYaml(buildInfo.getProfile());
    List<ContainerCommand> cmds = new ArrayList<>();
    for (ModuleDirectory mDir : mDirs) {
      mDir.validate();
      int testsPerContainer = mDir.isSetTestsPerContainer() ?
          Config.TESTS_PER_CONTAINER .getAsInt() :
          mDir.getTestsPerContainer();
      if (!mDir.getNeedsSplit() && mDir.isSetSingleTest() && !mDir.hasQFiles()) {
        // This is the simple case.  Remove any skipped tests and set any environment variables
        // and we're good
        MvnCommand mvn = new MvnCommand(containerClient.getContainerBaseDir() + "/" + mDir.getDir(),
            containerNumber++);
        setEnvs(mDir, mvn);
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
          if (!excludedTests.contains(testName)) tests.add(testName);
        }

        // deal with isolated tests
        if (mDir.isSetIsolatedTests()) {
          for (String test : mDir.getIsolatedTests()) {
            MvnCommand mvn = new MvnCommand(containerClient.getContainerBaseDir() + "/" + mDir.getDir(),
                containerNumber++);
            setEnvs(mDir, mvn);
            LOG.debug("Isolating test " + test + " in container " + (containerNumber - 1));
            cmds.add(mvn);
            tests.remove(test);
          }
        }

        while (!tests.isEmpty()) {
          MvnCommand mvn = new MvnCommand(containerClient.getContainerBaseDir() + "/" +
              mDir.getDir(), containerNumber++);
          setEnvs(mDir, mvn);
          for (int i = 0; i < testsPerContainer && !tests.isEmpty(); i++) {
            String single = tests.pop();
            LOG.debug("Adding test " + single + " to container " + (containerNumber - 1));
            mvn.addTest(single);
          }
          cmds.add(mvn);
        }
      } else if (mDir.isSetSingleTest()) {
        // Running a single test
        if (!mDir.hasQFiles()) {
          // One without any qfiles (or where all qfiles are run)
          MvnCommand mvn = new MvnCommand(containerClient.getContainerBaseDir() + "/" + mDir.getDir(),
                  containerNumber++);
          mvn.addTest(mDir.getSingleTest());
          setEnvs(mDir, mvn);
          cmds.add(mvn);
        } else {
          Set<String> qfiles;
          Set<String> excludedQFiles = new HashSet<>();
          // Figure out qfiles we need to skip
          if (mDir.isSetSkippedTests()) Collections.addAll(excludedQFiles, mDir.getSkippedQFiles());
          if (mDir.isSetQFilesDir()) {
            // If we're supposed to read the qfiles from a directory, do that
            qfiles = findQFilesInDir(containerClient, buildInfo.getLabel(), logger, mDir.getQFilesDir(),
                excludedQFiles);
          } else if (mDir.isSetQFilesProperties()) {
            // If we're supposed to read them from a properties list, do that
            qfiles = findQFilesFromProperties(mDir.getQFilesProperties());
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
              if (oneSet.size() > testsPerContainer) break;
              oneSet.add(qFile);
            }
            cmds.add(buildOneQFilesCmd(containerClient, oneSet, mDir));
            qfiles.removeAll(oneSet);
          }
        }
      } else {
        throw new InvalidObjectException("Help, I don't understand what you want me to do for " +
            "directory " + mDir.getDir());
      }
    }

    return cmds;
  }

  @VisibleForTesting
  List<ModuleDirectory> readYaml(String filename) throws IOException {
    URL yamlFile = getClass().getClassLoader().getResource(filename);
    if (yamlFile == null) {
      throw new IOException("Unable to find " + filename + " to determine tests to run");
    }
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    ObjectReader reader = mapper.readerFor(ModuleDirectory.class);
    MappingIterator<ModuleDirectory> iter = reader.readValues(yamlFile);
    return iter.readAll();
  }

  private void setEnvs(ModuleDirectory mDir, MvnCommand mvn) {
    if (mDir.getEnv() != null) {
      mvn.addEnvs(mDir.getEnv());
    }
  }

  private MvnCommand buildOneQFilesCmd(ContainerClient containerClient, Collection<String> qfiles,
                                       ModuleDirectory mDir) {
    MvnCommand mvn = new MvnCommand(containerClient.getContainerBaseDir() + "/" + mDir.getDir(),
        containerNumber++);
    mvn.setEnv("USER", DockerClient.USER);
    mvn.addTest(mDir.getSingleTest());
    for (String qfile : qfiles) {
      LOG.debug("Adding qfile " + qfile + " to container " + (containerNumber - 1));
      mvn.addQfile(qfile);
    }
    return mvn;
  }

  private String runContainer(ContainerClient containerClient, final String dir,
                              final String label,
                              final String containerName,
                              final String cmd, DTestLogger logger) throws IOException {
    ContainerResult result = containerClient.runContainer(300,
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
    return result.getLogs();
  }

  private Set<String> findQFilesFromProperties(String... properties) {
    Set<String> qfiles = new HashSet<>();
    for (String property : properties) {
      Collections.addAll(qfiles, testProperties.getProperty(property).trim().split(","));
    }
    return qfiles;
  }

  private Set<String> findQFilesInDir(ContainerClient containerClient, String label,
                                      DTestLogger logger, String qfileDir,
                                      Set<String> excludedQfiles) throws IOException {
    // Find all of the qfile tests
    String allPositiveQfiles = runContainer(containerClient, qfileDir, label,
        "qfile-finder-" + containerNumber++, "find . -name \\*.q -maxdepth 1", logger);


    Set<String> runnableQfiles = new HashSet<>();
    for (String line : allPositiveQfiles.split("\n")) {
      String testPath = line.trim();
      String[] pathElements = testPath.split("/");
      String testName = pathElements[pathElements.length - 1];
      if (!excludedQfiles.contains(testName)) runnableQfiles.add(testName);
    }
    return runnableQfiles;
  }
}
