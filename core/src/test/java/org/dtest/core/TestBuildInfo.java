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

import org.dtest.core.git.GitSource;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class TestBuildInfo {

  @Test
  public void simple() throws IOException {
    Config cfg = TestUtils.buildCfg(BuildInfo.CFG_BUILDINFO_LABEL, "patch1");
    BuildInfo info = new BuildInfo(System.getProperty("java.io.tmpdir"), new BuildYaml(), new GitSource(), true);
    info.setConfig(cfg).setLog(new TestUtils.TestLogger());
    info.checkLabelIsDockerable();
  }

  @Test
  public void withDash() throws IOException {
    Config cfg = TestUtils.buildCfg(BuildInfo.CFG_BUILDINFO_LABEL, "patch1-run2");
    BuildInfo info = new BuildInfo(System.getProperty("java.io.tmpdir"), new BuildYaml(), new GitSource(), true);
    info.setConfig(cfg).setLog(new TestUtils.TestLogger());
    info.checkLabelIsDockerable();
  }

  @Test(expected = IOException.class)
  public void withSlash() throws IOException {
    Config cfg = TestUtils.buildCfg(BuildInfo.CFG_BUILDINFO_LABEL, "patch1/run2");
    BuildInfo info = new BuildInfo(System.getProperty("java.io.tmpdir"), new BuildYaml(), new GitSource(), true);
    info.setConfig(cfg).setLog(new TestUtils.TestLogger());
    info.checkLabelIsDockerable();
  }

  @Test
  public void parseYaml() throws IOException {
    Config cfg = TestUtils.buildCfg(BuildInfo.CFG_BUILDINFO_LABEL, "parse-yaml",
                                    BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"));
    DTestLogger log = new TestUtils.TestLogger();
    BuildInfo info = new BuildInfo(TestUtils.getConfDir(), TestUtils.buildYaml(cfg, log), new GitSource(), true);
    info.setConfig(cfg).setLog(log);
    info.getBuildDir();
    BuildYaml yaml = info.getYaml();
    Assert.assertEquals("centos", yaml.getBaseImage());
    Assert.assertArrayEquals(new String[] {"java-1.8.0-openjdk-devel"}, yaml.getRequiredPackages());
    Assert.assertEquals("faky", yaml.getProjectName());
    Assert.assertEquals(5, yaml.getDirs().length);
    ModuleDirectory mDir = yaml.getDirs()[0];
    Assert.assertEquals("beeline", mDir.getDir());
    mDir = yaml.getDirs()[1];
    Assert.assertEquals("cli", mDir.getDir());
    Assert.assertEquals(1, mDir.getSkippedTests().length);
    Assert.assertEquals("TestCliDriverMethods", mDir.getSkippedTests()[0]);
    mDir = yaml.getDirs()[2];
    Assert.assertEquals("standalone-metastore", mDir.getDir());
    Assert.assertTrue(mDir.getNeedsSplit());
    Assert.assertEquals(1, mDir.getProperties().size());
    Assert.assertEquals("\"\"", mDir.getProperties().get("test.groups"));
    mDir = yaml.getDirs()[3];
    Assert.assertEquals("ql", mDir.getDir());
    Assert.assertTrue(mDir.getNeedsSplit());
    Assert.assertEquals(1, mDir.getSkippedTests().length);
    Assert.assertEquals("TestWorker", mDir.getSkippedTests()[0]);
    Assert.assertEquals(1, mDir.getIsolatedTests().length);
    Assert.assertEquals("TestCleaner2", mDir.getIsolatedTests()[0]);
    mDir = yaml.getDirs()[4];
    Assert.assertEquals("itests/qtest", mDir.getDir());
    Assert.assertEquals("TestContribCliDriver", mDir.getSingleTest());
  }
}
