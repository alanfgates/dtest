package org.dtest.hive;

import org.dtest.core.BuildInfo;
import org.dtest.core.BuildYaml;
import org.dtest.core.Config;
import org.dtest.core.DTestLogger;
import org.dtest.core.TestUtils;
import org.dtest.core.git.GitSource;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TestBuildInfoForHive {
  @Test
  public void parseYaml() throws IOException {
    Config cfg = TestUtils.buildCfg(BuildInfo.CFG_BUILDINFO_LABEL, "parse-yaml",
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildYaml.CFG_BUILDYAML_IMPL, HiveBuildYaml.class.getName());
    DTestLogger log = new TestUtils.TestLogger();
    BuildInfo info = new BuildInfo(TestUtils.getConfDir(), TestUtils.buildYaml(cfg, log), new GitSource(), true);
    info.setConfig(cfg).setLog(log);
    info.getBuildDir();
    BuildYaml yaml = info.getYaml();
    assert yaml instanceof HiveBuildYaml;
    HiveBuildYaml hiveYaml = (HiveBuildYaml)yaml;
    Assert.assertEquals("centos", yaml.getBaseImage());
    Assert.assertArrayEquals(new String[] {"java-1.8.0-openjdk-devel"}, yaml.getRequiredPackages());
    Assert.assertEquals("hive", yaml.getProjectName());
    Assert.assertEquals(7, hiveYaml.getHiveDirs().length);
    HiveModuleDirectory mDir = hiveYaml.getHiveDirs()[0];
    Assert.assertEquals("beeline", mDir.getDir());
    mDir = hiveYaml.getHiveDirs()[1];
    Assert.assertEquals("cli", mDir.getDir());
    Assert.assertEquals(1, mDir.getSkippedTests().length);
    Assert.assertEquals("TestCliDriverMethods", mDir.getSkippedTests()[0]);
    mDir = hiveYaml.getHiveDirs()[2];
    Assert.assertEquals("standalone-metastore", mDir.getDir());
    Assert.assertTrue(mDir.getNeedsSplit());
    Assert.assertEquals(1, mDir.getProperties().size());
    Assert.assertEquals("\"\"", mDir.getProperties().get("test.groups"));
    mDir = hiveYaml.getHiveDirs()[3];
    Assert.assertEquals("ql", mDir.getDir());
    Assert.assertTrue(mDir.getNeedsSplit());
    Assert.assertEquals(1, mDir.getSkippedTests().length);
    Assert.assertEquals("TestWorker", mDir.getSkippedTests()[0]);
    Assert.assertEquals(1, mDir.getIsolatedTests().length);
    Assert.assertEquals("TestCleaner2", mDir.getIsolatedTests()[0]);
    mDir = hiveYaml.getHiveDirs()[4];
    Assert.assertEquals("itests/qtest", mDir.getDir());
    Assert.assertEquals("TestContribCliDriver", mDir.getSingleTest());
    mDir = hiveYaml.getHiveDirs()[5];
    Assert.assertEquals("itests/qtest", mDir.getDir());
    Assert.assertEquals("TestMiniLlapLocalCliDriver", mDir.getSingleTest());
    Assert.assertEquals(2, mDir.getIncludedQFilesProperties().length);
    Assert.assertEquals("minillap.query.files", mDir.getIncludedQFilesProperties()[0]);
    Assert.assertEquals("minillap.shared.query.files", mDir.getIncludedQFilesProperties()[1]);
    Assert.assertEquals(1, mDir.getExcludedQFilesProperties().length);
    Assert.assertEquals("minitez.query.files", mDir.getExcludedQFilesProperties()[0]);
    Assert.assertEquals(1, mDir.getEnv().size());
    Assert.assertEquals("dtestuser", mDir.getEnv().get("USER"));
    Assert.assertEquals(1, mDir.getProperties().size());
    Assert.assertTrue(mDir.getProperties().containsKey("skipSparkTests"));
    mDir = hiveYaml.getHiveDirs()[6];
    Assert.assertEquals("itests/qtest", mDir.getDir());
    Assert.assertEquals("TestCliDriver", mDir.getSingleTest());
    Assert.assertEquals("ql/src/test/queries/clientpositive", mDir.getQFilesDir());
    Assert.assertEquals(4, mDir.getTestsPerContainer());
    Assert.assertEquals(1, mDir.getIsolatedQFiles().length);
    Assert.assertEquals("authorization_show_grant.q", mDir.getIsolatedQFiles()[0]);
    Assert.assertEquals(3, mDir.getExcludedQFilesProperties().length);
    Assert.assertEquals("minillap.query.files", mDir.getExcludedQFilesProperties()[0]);
    Assert.assertEquals("minillap.shared.query.files", mDir.getExcludedQFilesProperties()[1]);
    Assert.assertEquals("minitez.query.files", mDir.getExcludedQFilesProperties()[2]);
    Assert.assertEquals(2, mDir.getExcludedQFiles().length);
    Assert.assertEquals("masking_5.q", mDir.getExcludedQFiles()[0]);
    Assert.assertEquals("orc_merge10.q", mDir.getExcludedQFiles()[1]);
  }

  @Test
  public void masterYaml() throws IOException {
    // Don't confirm all the contents, as I assume these will change, but make sure we can at least parse the
    // master yaml file
    Config cfg = TestUtils.buildCfg(BuildInfo.CFG_BUILDINFO_LABEL, "parse-master-yaml",
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildYaml.CFG_BUILDYAML_IMPL, HiveBuildYaml.class.getName());
    String cfgDir = System.getProperty("java.io.tmpdir") + File.separator + "classes" + File.separator + "master";
    DTestLogger log = new TestUtils.TestLogger();
    BuildInfo info = new BuildInfo(cfgDir, BuildYaml.readYaml(cfgDir, cfg, log, null, null), new GitSource(), true);
    info.setConfig(cfg).setLog(log);
    info.getBuildDir();
    BuildYaml yaml = info.getYaml();
    assert yaml instanceof HiveBuildYaml;
    Assert.assertEquals("hive master yaml file", yaml.getComment());
  }

  @Test
  public void branch3Yaml() throws IOException {
    // Don't confirm all the contents, as I assume these will change, but make sure we can at least parse the
    // branch3 yaml file
    Config cfg = TestUtils.buildCfg(BuildInfo.CFG_BUILDINFO_LABEL, "parse-branch3-yaml",
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildYaml.CFG_BUILDYAML_IMPL, HiveBuildYaml.class.getName());
    String cfgDir = System.getProperty("java.io.tmpdir") + File.separator + "classes" + File.separator + "branch3";
    DTestLogger log = new TestUtils.TestLogger();
    BuildInfo info = new BuildInfo(cfgDir, BuildYaml.readYaml(cfgDir, cfg, log, null, null), new GitSource(), true);
    info.setConfig(cfg).setLog(log);
    info.getBuildDir();
    BuildYaml yaml = info.getYaml();
    assert yaml instanceof HiveBuildYaml;
    Assert.assertEquals("hive branch-3 yaml file", yaml.getComment());
  }
}
