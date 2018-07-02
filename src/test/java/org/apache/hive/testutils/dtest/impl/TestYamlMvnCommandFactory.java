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

import org.apache.commons.lang3.StringUtils;
import org.apache.hive.testutils.dtest.BuildInfo;
import org.apache.hive.testutils.dtest.Config;
import org.apache.hive.testutils.dtest.ContainerClient;
import org.apache.hive.testutils.dtest.ContainerCommand;
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
    mDir = mDirs.get(5);
    Assert.assertEquals("itests/qtest", mDir.getDir());
    Assert.assertEquals("TestMiniLlapLocalCliDriver", mDir.getSingleTest());
    Assert.assertEquals(2, mDir.getQFilesProperties().length);
    Assert.assertEquals("minillap.query.files", mDir.getQFilesProperties()[0]);
    Assert.assertEquals("minillap.shared.query.files", mDir.getQFilesProperties()[1]);
    Assert.assertEquals(1, mDir.getEnv().size());
    Assert.assertEquals("dtestuser", mDir.getEnv().get("USER"));
    Assert.assertEquals(1, mDir.getMvnProperties().size());
    Assert.assertTrue(mDir.getMvnProperties().containsKey("skipSparkTests"));
    mDir = mDirs.get(6);
    Assert.assertEquals("itests/qtest", mDir.getDir());
    Assert.assertEquals("TestCliDriver", mDir.getSingleTest());
    Assert.assertEquals("ql/src/test/queries/clientpositive", mDir.getQFilesDir());
    Assert.assertEquals(4, mDir.getTestsPerContainer());
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

  @Test
  public void parseMasterYamlProfile() throws IOException {
    // I won't verify the contents but at least parse it
    YamlMvnCommandFactory factory = new YamlMvnCommandFactory();
    List<ModuleDirectory> mDirs = factory.readYaml("master");
    Assert.assertTrue(mDirs.size() > 0);
  }

  @Test
  public void parseBranch3YamlProfile() throws IOException {
    // I won't verify the contents but at least parse it
    YamlMvnCommandFactory factory = new YamlMvnCommandFactory();
    List<ModuleDirectory> mDirs = factory.readYaml("branch-3-profile.yaml");
    Assert.assertTrue(mDirs.size() > 0);
  }

  @Test
  public void buildCommands() throws IOException {
    YamlMvnCommandFactory factory = new YamlMvnCommandFactory();
    BuildInfo buildInfo = new BuildInfo("mybranch", "http://myrepo.com/repo.git", "mylabel",
        "test-profile.yaml");
    DTestLogger logger = new DTestLogger(".");
    List<ContainerCommand> cmds = factory.getContainerCommands(new TestContainerClient(),
        buildInfo, logger);
    Assert.assertEquals(11, cmds.size());
    Assert.assertEquals("/bin/bash -c ( cd /tmp/beeline; /usr/bin/mvn test -Dsurefire.timeout=5400)", StringUtils.join(cmds.get(0).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/cli; /usr/bin/mvn test -Dsurefire.timeout=5400 -Dtest.excludes.additional=**/TestCliDriverMethods)", StringUtils.join(cmds.get(1).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/standalone-metastore; /usr/bin/mvn test -Dsurefire.timeout=5400 -Dtest=TestRetriesInRetryingHMSHandler,TestRetryingHMSHandler,TestSetUGIOnBothClientServer,TestSetUGIOnOnlyClient,TestSetUGIOnOnlyServer,TestStats,TestMetastoreSchemaTool,TestSchemaToolForMetastore,TestTxnHandlerNegative,TestTxnUtils -Dtest.groups=\"\")", StringUtils.join(cmds.get(2).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/standalone-metastore; /usr/bin/mvn test -Dsurefire.timeout=5400 -Dtest=TestHdfsUtils,TestMetaStoreUtils -Dtest.groups=\"\")", StringUtils.join(cmds.get(3).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/ql; /usr/bin/mvn test -Dsurefire.timeout=5400 -Dtest=TestCleaner2)", StringUtils.join(cmds.get(4).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/ql; /usr/bin/mvn test -Dsurefire.timeout=5400 -Dtest=CompactorTest,TestCleaner,TestInitiator,TestWorker2)", StringUtils.join(cmds.get(5).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/itests/qtest; /usr/bin/mvn test -Dsurefire.timeout=5400 -Dtest=TestContribCliDriver -DskipSparkTests)", StringUtils.join(cmds.get(6).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/itests/qtest; USER=dtestuser /usr/bin/mvn test -Dsurefire.timeout=5400 -Dtest=TestMiniLlapLocalCliDriver -Dqfile=llapdecider.q,acid_bucket_pruning.q,insert_into1.q,bucket6.q -DskipSparkTests)", StringUtils.join(cmds.get(7).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/itests/qtest; /usr/bin/mvn test -Dsurefire.timeout=5400 -Dtest=TestCliDriver -Dqfile=authorization_show_grant.q -DskipSparkTests)", StringUtils.join(cmds.get(8).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/itests/qtest; /usr/bin/mvn test -Dsurefire.timeout=5400 -Dtest=TestCliDriver -Dqfile=masking_acid_no_masking.q,masking_8.q,masking_9.q,masking_6.q -DskipSparkTests)", StringUtils.join(cmds.get(9).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd /tmp/itests/qtest; /usr/bin/mvn test -Dsurefire.timeout=5400 -Dtest=TestCliDriver -Dqfile=masking_7.q -DskipSparkTests)", StringUtils.join(cmds.get(10).shellCommand(), " "));
  }

  private static class TestContainerClient implements ContainerClient {
    @Override
    public void defineImage(String dir, String repo, String branch, String label) throws
        IOException {

    }

    @Override
    public String getContainerBaseDir() {
      return "/tmp";
    }

    @Override
    public ContainerResult runContainer(long toWait, ContainerCommand cmd,
                                        DTestLogger logger) throws IOException {
      // Doing our own mocking here
      String shellCmd = StringUtils.join(cmd.shellCommand(), " ");
      if (shellCmd.contains("cat itests/src/test/resources/testconfiguration.properties")) {
        return new ContainerResult(cmd, 0, "minillap.query.files=acid_bucket_pruning.q,\\\n" +
            "  bucket6.q\n" +
            "minillap.shared.query.files=insert_into1.q,\\\n" +
            " llapdecider.q\n");
      } else if (shellCmd.contains("standalone-metastore") && shellCmd.contains("find")) {
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
      } else if (shellCmd.contains("find") && shellCmd.contains("clientpositive")) {
        return new ContainerResult(cmd, 0, "ql/src/test/queries/clientpositive//authorization_show_grant.q\n" +
            "ql/src/test/queries/clientpositive//masking_5.q\n" +
            "ql/src/test/queries/clientpositive//masking_6.q\n" +
            "ql/src/test/queries/clientpositive//masking_7.q\n" +
            "ql/src/test/queries/clientpositive//masking_8.q\n" +
            "ql/src/test/queries/clientpositive//masking_9.q\n" +
            "ql/src/test/queries/clientpositive//masking_acid_no_masking.q\n");
      } else {
        throw new RuntimeException("Unexpected cmd " + shellCmd);
      }
    }
  }

}
