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

import org.junit.Test;

import java.io.InvalidObjectException;

public class TestModuleDirectory {

  @Test(expected = InvalidObjectException.class)
  public void splitSingle() throws InvalidObjectException {
    ModuleDirectory dir = new ModuleDirectory();
    dir.setNeedsSplit(true);
    dir.setSingleTest("a");
    dir.validate();
  }

  @Test(expected = InvalidObjectException.class)
  public void qfilesNonSingle() throws InvalidObjectException {
    ModuleDirectory dir = new ModuleDirectory();
    dir.setQFiles(new String[] {"a", "b"});
    dir.validate();
  }

  @Test(expected = InvalidObjectException.class)
  public void qfilesAndQFilesDir() throws InvalidObjectException {
    ModuleDirectory dir = new ModuleDirectory();
    dir.setQFiles(new String[] {"a", "b"});
    dir.setQFilesDir("a");
    dir.validate();
  }

  @Test(expected = InvalidObjectException.class)
  public void qfilesAndQFilesProperties() throws InvalidObjectException {
    ModuleDirectory dir = new ModuleDirectory();
    dir.setQFiles(new String[] {"a", "b"});
    dir.setQFilesProperties(new String[] {"a", "b"});
    dir.validate();
  }

  @Test(expected = InvalidObjectException.class)
  public void qfilesPropertiesAndQFilesDir() throws InvalidObjectException {
    ModuleDirectory dir = new ModuleDirectory();
    dir.setQFilesProperties(new String[] {"a", "b"});
    dir.setQFilesDir("a");
    dir.validate();
  }
}
