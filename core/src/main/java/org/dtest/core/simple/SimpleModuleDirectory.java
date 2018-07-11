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
package org.dtest.core.simple;

import java.io.InvalidObjectException;
import java.util.Map;

public class SimpleModuleDirectory {
  private String   dir;                // build directory
  private boolean  needsSplit;         // if false, run all tests in single container, else split them up
  private int      testsPerContainer;  // if needsSplit = true, how many tests per container to run
  private String[] isolatedTests;      // tests to run in a separate container
  private String   singleTest;         // if set, run only this test
  private String[] skippedTests;       // tests to skip
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
  protected void validate() throws InvalidObjectException {
    if (dir == null) {
      throw new InvalidObjectException("You must specify a directory");
    }
    if (needsSplit && singleTest != null) {
      throw new InvalidObjectException("You cannot specify a split on a single test, " + dir);
    }
  }
}
