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

import org.dtest.core.ModuleDirectory;

import java.io.InvalidObjectException;

/**
 * A container for information about tests to be run in a maven module.
 */
public class HiveModuleDirectory extends ModuleDirectory {
  private String[] qFiles;                   // list of qfiles to run
  private String[] excludedQFiles;           // list of qfiles to exclude
  private String[] includedQFilesProperties; // if set, the list of included qfiles will be
                                             // determined by reading the testconfigurations
                                             // .properties file more than one property can be
                                             // read from the file
  private String[] excludedQFilesProperties; // if set, the list of excluded qfiles will be
                                             // determined by reading the testconfigurations
                                             // .properties file more than one property can be
                                             // read from the file
  private String   qFilesDir;                // if set, all qfiles in this directory will be used
  private String[] isolatedQFiles;           // Any qfiles that need to be run alone

  public String[] getQFiles() {
    return qFiles;
  }

  // If this can get qfile in any way, this will return true
  public boolean hasQFiles() {
    return qFiles != null || qFilesDir != null || includedQFilesProperties != null;
  }

  public void setQFiles(String[] qFiles) {
    this.qFiles = qFiles;
  }

  public String[] getExcludedQFiles() {
    return excludedQFiles;
  }

  public boolean isSetExcludedQFiles() {
    return excludedQFiles != null;
  }

  public void setExcludedQFiles(String[] skippedQFiles) {
    this.excludedQFiles = skippedQFiles;
  }

  public String[] getIncludedQFilesProperties() {
    return includedQFilesProperties;
  }

  public boolean isSetIncludedQFilesProperties() {
    return includedQFilesProperties != null;
  }

  public void setIncludedQFilesProperties(String[] includedQFilesProperties) {
    this.includedQFilesProperties = includedQFilesProperties;
  }

  public String[] getExcludedQFilesProperties() {
    return excludedQFilesProperties;
  }

  public boolean isSetExcludedQFilesProperties() {
    return excludedQFilesProperties != null;
  }

  public void setExcludedQFilesProperties(String[] excludedQFilesProperties) {
    this.excludedQFilesProperties = excludedQFilesProperties;
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

  /**
   * It's possible in YAML to build an invalid version of one of these (for example, something
   * that says to split the tests but then says to run only one test).  Ideally we'd deal with
   * this with subclasses, but given that we're reading from a config file it's easier to let one
   * object hold all the options and then validate that users don't do something silly.
   * @throws InvalidObjectException if the object is invalid
   */
  @Override
  public void validate() throws InvalidObjectException {
    super.validate();
    if (getSingleTest() == null && hasQFiles()) {
      throw new InvalidObjectException("You cannot specify qfiles for more than one test, " + getDir());
    }
    if (qFiles != null && qFilesDir != null) {
      throw new InvalidObjectException("You cannot specify a list of qfiles and a directory to " +
          "read qfiles from, " + getDir());
    }
    if (qFiles != null && includedQFilesProperties != null) {
      throw new InvalidObjectException("You cannot specify a list of qfiles and a set of " +
          "properties to read qfiles from, " + getDir());
    }
    if (qFilesDir != null && includedQFilesProperties != null) {
      throw new InvalidObjectException("You cannot specify a qfile directory and a set of " +
          "properties to read qfiles from, " + getDir());
    }
  }
}
