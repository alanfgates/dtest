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

import java.io.InvalidObjectException;
import java.util.Map;

/**
 * Information from {@link BuildYaml} specific to each set of tests in the build.  Should probably be named
 * TestGroup or something since the directory is not unique.
 */
public class ModuleDirectory {

  private String dir;
  private boolean needsSplit;
  private int testsPerContainer;
  private String[] isolatedTests;
  private String singleTest;
  private String[] skippedTests;
  private Map<String, String> env;
  private Map<String, String> properties;

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

  public Map<String, String> getEnv() {
    return env;
  }

  public void setEnv(Map<String, String> env) {
    this.env = env;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  /**
   * It's possible in YAML to build an invalid version of one of these (for example, something
   * that says to split the tests but then says to run only one test).  Ideally we'd deal with
   * this with subclasses, but given that we're reading from a config file it's easier to let one
   * object hold all the options and then validate that users don't do something silly.
   * @throws InvalidObjectException if the object is invalid
   */
  public void validate() throws InvalidObjectException {
    if (dir == null) {
      throw new InvalidObjectException("You must specify a directory");
    }
    if (needsSplit && singleTest != null) {
      throw new InvalidObjectException("You cannot specify a split on a single test, " + dir);
    }
  }

}
