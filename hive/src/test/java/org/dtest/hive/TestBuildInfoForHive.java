package org.dtest.hive;

import org.dtest.core.BuildInfo;
import org.dtest.core.BuildYaml;
import org.dtest.core.Config;
import org.dtest.core.DTestLogger;
import org.dtest.core.testutils.TestUtils;
import org.dtest.core.git.GitSource;
import org.dtest.core.testutils.TestLogger;
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
    DTestLogger log = new TestLogger();
    BuildInfo info = new BuildInfo(TestUtils.buildYaml(cfg, log, "hivetest"), new GitSource(), true, "5");
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

  // Test reading the actual Hive master yaml file.  This uses the master.yaml file from hive/src/main/resources
  // rather than hivetest.yaml from hive/src/test/resources
  @Test
  public void masterYaml() throws IOException {
    readYaml("parse-master-yaml", "master", "master", "hive master yaml file");
  }

  // Test reading the actual Hive branch yaml file.  This uses the master.yaml file from hive/src/main/resources
  // rather than hivetest.yaml from hive/src/test/resources
  @Test
  public void branch3Yaml() throws IOException {
    readYaml("parse-branch-3-yaml", "branch-3", "branch-3", "hive branch-3 yaml file");
  }

  @Test
  public void branch31Yaml() throws IOException {
    readYaml("parse-branch-31-yaml", "branch-3.1", "branch-3.1", "hive branch-3.1 yaml file");
  }

  @Test
  public void branch2Yaml() throws IOException {
    readYaml("parse-branch-2-yaml", "branch-2", "branch-2", "hive branch-2 yaml file");
  }

  @Test
  public void branch23() throws IOException {
    readYaml("parse-branch-23-yaml", "branch-2.3", "branch-2.3", "hive branch-2.3 yaml file");
  }

  private void readYaml(String label, String profile, String branch, String expectedComment) throws IOException {
    Config cfg = TestUtils.buildCfg(BuildInfo.CFG_BUILDINFO_LABEL, label,
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildYaml.CFG_BUILDYAML_IMPL, HiveBuildYaml.class.getName());
    File cfgDir = new File(System.getProperty("java.io.tmpdir"), "classes");
    DTestLogger log = new TestLogger();
    BuildInfo info = new BuildInfo(BuildYaml.readYaml(cfgDir, cfg, log, null, profile, branch), new GitSource(), true, "7");
    info.setConfig(cfg).setLog(log);
    info.getBuildDir();
    BuildYaml yaml = info.getYaml();
    assert yaml instanceof HiveBuildYaml;
    Assert.assertEquals(expectedComment, yaml.getComment());

  }
}
