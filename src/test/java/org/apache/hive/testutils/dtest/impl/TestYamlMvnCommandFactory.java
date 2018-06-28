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

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TestYamlMvnCommandFactory {

  @Test
  public void parseYaml() throws IOException {
    YamlMvnCommandFactory factory = new YamlMvnCommandFactory();
    List<ModuleDirectory> mDirs = factory.readYaml("test-profile.yaml");
    Assert.assertEquals(7, mDirs.size());
    ModuleDirectory mDir = mDirs.get(0);
    Assert.assertEquals("beeline", mDir.getDir());
    mDir = mDirs.get(1);
    Assert.assertEquals("cli", mDir.getDir());
    Assert.assertEquals(1, mDir.getSkippedTests().length);
    Assert.assertEquals("TestCliDriverMethods", mDir.getSkippedTests()[0]);
    mDir = mDirs.get(2);
    Assert.assertEquals("standalone-metastore", mDir.getDir());
    Assert.assertTrue(mDir.getNeedsSplit());
    mDir = mDirs.get(3);
    Assert.assertEquals("ql", mDir.getDir());
    Assert.assertTrue(mDir.getNeedsSplit());
    Assert.assertEquals(1, mDir.getSkippedTests().length);
    Assert.assertEquals("TestWorker", mDir.getSkippedTests()[0]);
    Assert.assertEquals(1, mDir.getIsolatedTests().length);
    Assert.assertEquals("TestCompactor2", mDir.getIsolatedTests()[0]);
    mDir = mDirs.get(4);
    Assert.assertEquals("itests/qtest", mDir.getDir());
    Assert.assertEquals("TestContribCliDriver", mDir.getSingleTest());
    mDir = mDirs.get(5);
    Assert.assertEquals("itests/qtest", mDir.getDir());
    Assert.assertEquals("TestMiniLlapLocalCliDriver", mDir.getSingleTest());
    Assert.assertEquals(2, mDir.getQFilesProperties().length);
    Assert.assertEquals("minillaplocal.query.files", mDir.getQFilesProperties()[0]);
    Assert.assertEquals("minillaplocal.shared.query.files", mDir.getQFilesProperties()[1]);
    Assert.assertEquals(1, mDir.getEnv().size());
    Assert.assertEquals("dtestuser", mDir.getEnv().get("USER"));
    mDir = mDirs.get(6);
    Assert.assertEquals("itests/qtest", mDir.getDir());
    Assert.assertEquals("TestCliDriver", mDir.getSingleTest());
    Assert.assertEquals("ql/src/test/queries/clientpositive", mDir.getQFilesDir());
    Assert.assertEquals(5, mDir.getTestsPerContainer());
    Assert.assertEquals(1, mDir.getIsolatedQFiles().length);
    Assert.assertEquals("authorization_show_grant.q", mDir.getIsolatedQFiles()[0]);
    Assert.assertEquals(2, mDir.getSkippedQFiles().length);
    Assert.assertEquals("masking_5.q", mDir.getSkippedQFiles()[0]);
    Assert.assertEquals("orc_merge10.q", mDir.getSkippedQFiles()[1]);
  }

  @Test(expected = IOException.class)
  public void nonExistentYamlFile() throws IOException {
    YamlMvnCommandFactory factory = new YamlMvnCommandFactory();
    List<ModuleDirectory> mDirs = factory.readYaml("nosuch-profile.yaml");
  }

}
