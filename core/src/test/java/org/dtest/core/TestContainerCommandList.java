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

import org.apache.commons.lang3.StringUtils;
import org.dtest.core.git.GitSource;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TestContainerCommandList {

  @BeforeClass
  public static void createConfFile() throws IOException {
    TestUtils.createConfFile();
  }

  @Test
  public void parseYaml() throws IOException {
    BaseContainerCommandList factory = new BaseContainerCommandList();
    List<BaseModuleDirectory> mDirs = factory.readYaml(TestUtils.getConfDir(), BaseModuleDirectory.class);
    Assert.assertEquals(5, mDirs.size());
    BaseModuleDirectory mDir = mDirs.get(0);
    Assert.assertEquals("beeline", mDir.getDir());
    mDir = mDirs.get(1);
    Assert.assertEquals("cli", mDir.getDir());
    Assert.assertEquals(1, mDir.getSkippedTests().length);
    Assert.assertEquals("TestCliDriverMethods", mDir.getSkippedTests()[0]);
    mDir = mDirs.get(2);
    Assert.assertEquals("standalone-metastore", mDir.getDir());
    Assert.assertTrue(mDir.getNeedsSplit());
    Assert.assertEquals(1, mDir.getMvnProperties().size());
    Assert.assertEquals("\"\"", mDir.getMvnProperties().get("test.groups"));
    mDir = mDirs.get(3);
    Assert.assertEquals("ql", mDir.getDir());
    Assert.assertTrue(mDir.getNeedsSplit());
    Assert.assertEquals(1, mDir.getSkippedTests().length);
    Assert.assertEquals("TestWorker", mDir.getSkippedTests()[0]);
    Assert.assertEquals(1, mDir.getIsolatedTests().length);
    Assert.assertEquals("TestCleaner2", mDir.getIsolatedTests()[0]);
    mDir = mDirs.get(4);
    Assert.assertEquals("itests/qtest", mDir.getDir());
    Assert.assertEquals("TestContribCliDriver", mDir.getSingleTest());
  }

  @Test(expected = IOException.class)
  public void nonExistentYamlFile() throws IOException {
    BaseContainerCommandList factory = new BaseContainerCommandList();
    List<BaseModuleDirectory> mDirs = factory.readYaml("nosuch", BaseModuleDirectory.class);
  }

  @Test
  public void buildCommands() throws IOException {
    BaseContainerCommandList cmds = new BaseContainerCommandList();
    BuildInfo buildInfo = new BuildInfo(TestUtils.getConfDir(), new GitSource(), "mylabel");
    DTestLogger logger = new DTestLogger(".");
    cmds.buildContainerCommands(new TestContainerClient(), buildInfo, logger);
    Assert.assertEquals(7, cmds.size());
    Assert.assertEquals("/bin/bash -c ( cd /tmp/beeline; /usr/bin/mvn test -Dsurefire.timeout=300)", StringUtils.join(cmds.get(0).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/cli; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest.excludes.additional=**/TestCliDriverMethods)", StringUtils.join(cmds.get(1).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/standalone-metastore; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestRetriesInRetryingHMSHandler,TestRetryingHMSHandler,TestSetUGIOnBothClientServer,TestSetUGIOnOnlyClient,TestSetUGIOnOnlyServer,TestStats,TestMetastoreSchemaTool,TestSchemaToolForMetastore,TestTxnHandlerNegative,TestTxnUtils -Dtest.groups=\"\")", StringUtils.join(cmds.get(2).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/standalone-metastore; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestHdfsUtils,TestMetaStoreUtils -Dtest.groups=\"\")", StringUtils.join(cmds.get(3).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/ql; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestCleaner2)", StringUtils.join(cmds.get(4).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/ql; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=CompactorTest,TestCleaner,TestInitiator,TestWorker2)", StringUtils.join(cmds.get(5).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/itests/qtest; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestContribCliDriver -DskipSparkTests)", StringUtils.join(cmds.get(6).shellCommand(), " "));
  }

  private static class TestContainerClient extends ContainerClient {

    @Override
    public void buildImage(String dir, DTestLogger logger) throws IOException {

    }

    @Override
    public void copyLogFiles(ContainerResult result, String targetDir, DTestLogger logger) throws IOException {

    }

    @Override
    public void removeContainer(ContainerResult result, DTestLogger logger) throws IOException {

    }

    @Override
    public void removeImage(DTestLogger logger) throws IOException {

    }

    @Override
    public void defineImage() throws IOException {

    }

    @Override
    public String getContainerBaseDir() {
      return "/tmp";
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd,
                                        DTestLogger logger) throws IOException {
      // Doing our own mocking here
      String shellCmd = StringUtils.join(cmd.shellCommand(), " ");
      if (shellCmd.contains("standalone-metastore") && shellCmd.contains("find")) {
        return new ContainerResult(cmd, 0, "standalone-metastore/src/test/java//org/apache/hadoop/hive/metastore/TestRetriesInRetryingHMSHandler.java\n" +
            "standalone-metastore/src/test/java//org/apache/hadoop/hive/metastore/TestRetryingHMSHandler.java\n" +
            "standalone-metastore/src/test/java//org/apache/hadoop/hive/metastore/TestSetUGIOnBothClientServer.java\n" +
            "standalone-metastore/src/test/java//org/apache/hadoop/hive/metastore/TestSetUGIOnOnlyClient.java\n" +
            "standalone-metastore/src/test/java//org/apache/hadoop/hive/metastore/TestSetUGIOnOnlyServer.java\n" +
            "standalone-metastore/src/test/java//org/apache/hadoop/hive/metastore/TestStats.java\n" +
            "standalone-metastore/src/test/java//org/apache/hadoop/hive/metastore/tools/TestMetastoreSchemaTool.java\n" +
            "standalone-metastore/src/test/java//org/apache/hadoop/hive/metastore/tools/TestSchemaToolForMetastore.java\n" +
            "standalone-metastore/src/test/java//org/apache/hadoop/hive/metastore/txn/TestTxnHandlerNegative.java\n" +
            "standalone-metastore/src/test/java//org/apache/hadoop/hive/metastore/txn/TestTxnUtils.java\n" +
            "standalone-metastore/src/test/java//org/apache/hadoop/hive/metastore/utils/TestHdfsUtils.java\n" +
            "standalone-metastore/src/test/java//org/apache/hadoop/hive/metastore/utils/TestMetaStoreUtils.java\n");
      } else if (shellCmd.contains("ql") && shellCmd.contains("find") && !shellCmd.contains("clientpositive")) {
        return new ContainerResult(cmd, 0, "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/CompactorTest.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestCleaner.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestCleaner2.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestInitiator.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestWorker.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestWorker2.java\n");
      } else {
        throw new RuntimeException("Unexpected cmd " + shellCmd);
      }
    }

    @Override
    public String getProjectName() {
      return null;
    }
  }

}
