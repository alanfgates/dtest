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

import java.io.InvalidObjectException;
import java.util.Map;

/**
 * A container for information about tests to be run in a maven module.
 */
public class ModuleDirectory {
  private String   dir;                // build directory
  private boolean  needsSplit;         // if false, run all tests in single container, else split them up
  private int      testsPerContainer;  // if needsSplit = true, how many tests per container to run
  private String[] isolatedTests;      // tests to run in a separate container
  private String   singleTest;         // if set, run only this test
  private String[] skippedTests;       // tests to skip
  private String[] qFiles;             // list of qfiles to run
  private String[] skippedQFiles;      // list of qfiles to skip
  private String[] qFilesProperties;   // if set, the list of qfiles will be determined by
                                       // reading the testconfigurations.properties file
                                       // more than one property can be read from the file
  private String   qFilesDir;          // if set, all qfiles in this directory will be used
  private String[] isolatedQFiles;     // Any qfiles that need to be run alone
  private Map<String, String> env;     // environment variables to set when running the mvn cmd
  private Map<String, String> mvnProperties;  // properties to set via -D when running mvn cmd

  public String getDir() {
    return dir;
  }

  public void setDir(String dir) {
    this.dir = dir;
  }

  public boolean getNeedsSplit() {
    return needsSplit;
  }

  public void setNeedsSplit(boolean needsSplit) {
    this.needsSplit = needsSplit;
  }

  public int getTestsPerContainer() {
    return testsPerContainer;
  }

  public boolean isSetTestsPerContainer() {
    return testsPerContainer != 0;
  }

  public void setTestsPerContainer(int testsPerContainer) {
    this.testsPerContainer = testsPerContainer;
  }

  public String[] getIsolatedTests() {
    return isolatedTests;
  }

  public boolean isSetIsolatedTests() {
    return isolatedTests != null;
  }

  public void setIsolatedTests(String[] isolatedTests) {
    this.isolatedTests = isolatedTests;
  }

  public String getSingleTest() {
    return singleTest;
  }

  public boolean isSetSingleTest() {
    return singleTest != null;
  }

  public void setSingleTest(String singleTest) {
    this.singleTest = singleTest;
  }

  public String[] getSkippedTests() {
    return skippedTests;
  }

  public boolean isSetSkippedTests() {
    return skippedTests != null;
  }

  public void setSkippedTests(String[] skippedTests) {
    this.skippedTests = skippedTests;
  }

  public String[] getQFiles() {
    return qFiles;
  }

  // If this can get qfile in any way, this will return true
  public boolean hasQFiles() {
    return qFiles != null || qFilesDir != null || qFilesProperties != null;
  }

  public void setQFiles(String[] qFiles) {
    this.qFiles = qFiles;
  }

  public String[] getSkippedQFiles() {
    return skippedQFiles;
  }

  public boolean isSetSkippedQFiles() {
    return skippedQFiles != null;
  }

  public void setSkippedQFiles(String[] skippedQFiles) {
    this.skippedQFiles = skippedQFiles;
  }

  public String[] getQFilesProperties() {
    return qFilesProperties;
  }

  public boolean isSetQFilesProperties() {
    return qFilesProperties != null;
  }

  public void setQFilesProperties(String[] qFilesProperties) {
    this.qFilesProperties = qFilesProperties;
  }

  public String getQFilesDir() {
    return qFilesDir;
  }

  public boolean isSetQFilesDir() {
    return qFilesDir != null;
  }

  public void setQFilesDir(String qFilesDir) {
    this.qFilesDir = qFilesDir;
  }

  public String[] getIsolatedQFiles() {
    return isolatedQFiles;
  }

  public boolean isSetIsolatedQFiles() {
    return isolatedQFiles != null;
  }

  public void setIsolatedQFiles(String[] isolatedQFiles) {
    this.isolatedQFiles = isolatedQFiles;
  }

  public Map<String, String> getEnv() {
    return env;
  }

  public void setEnv(Map<String, String> env) {
    this.env = env;
  }

  public Map<String, String> getMvnProperties() {
    return mvnProperties;
  }

  public void setMvnProperties(Map<String, String> mvnProperties) {
    this.mvnProperties = mvnProperties;
  }

  /**
   * It's possible in YAML to build an invalid version of one of these (for example, something
   * that says to split the tests but then says to run only one test).  Ideally we'd deal with
   * this with subclasses, but given that we're reading from a config file it's easier to let one
   * object hold all the options and then validate that users don't do something silly.
   * @throws InvalidObjectException if the object is invalid
   */
  void validate() throws InvalidObjectException {
    if (dir == null) {
      throw new InvalidObjectException("You must specify a directory");
    }
    if (needsSplit && singleTest != null) {
      throw new InvalidObjectException("You cannot specify a split on a single test, " + dir);
    }
    if (singleTest == null && hasQFiles()) {
      throw new InvalidObjectException("You cannot specify qfiles for more than one test, " + dir);
    }
    if (qFiles != null && qFilesDir != null) {
      throw new InvalidObjectException("You cannot specify a list of qfiles and a directory to " +
          "read qfiles from, " + dir);
    }
    if (qFiles != null && qFilesProperties != null) {
      throw new InvalidObjectException("You cannot specify a list of qfiles and a list of " +
          "properties to read qfiles from, " + dir);
    }
    if (qFilesDir != null && qFilesProperties != null) {
      throw new InvalidObjectException("You cannot specify a directory to read qfiles from and a " +
          "list of properties to read qfiles from, " + dir);
    }
  }
}
