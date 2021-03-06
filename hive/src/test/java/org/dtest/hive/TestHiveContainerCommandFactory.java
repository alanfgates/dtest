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

import org.apache.commons.lang3.StringUtils;
import org.dtest.core.BuildInfo;
import org.dtest.core.BuildYaml;
import org.dtest.core.Config;
import org.dtest.core.ContainerCommand;
import org.dtest.core.ContainerResult;
import org.dtest.core.testutils.TestUtilities;
import org.dtest.core.git.GitSource;
import org.dtest.core.testutils.MockContainerClient;
import org.dtest.core.testutils.TestLogger;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

public class TestHiveContainerCommandFactory {


  @Test
  public void buildCommands() throws IOException {
    File buildDir = TestUtilities.createBuildDir();
    TestLogger log = new TestLogger();
    Config cfg = TestUtilities.buildCfg(
        BuildInfo.CFG_BUILDINFO_LABEL, "mylabel",
        BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"),
        BuildYaml.CFG_BUILDYAML_IMPL, HiveBuildYaml.class.getName());
    HiveContainerCommandFactory cmds = new HiveContainerCommandFactory();
    cmds.setConfig(cfg).setLog(log);
    BuildInfo buildInfo = new BuildInfo(TestUtilities.buildYaml(cfg, log, "hivetest"), new GitSource(), true, "1");
    buildInfo.setConfig(cfg).setLog(log);
    buildInfo.getBuildDir();
    cmds.buildContainerCommands(new TestContainerClient("hive-container-cmd-build", "allgood", buildDir, 0), buildInfo);
    log.dumpToLog();
    Assert.assertEquals(11, cmds.getCmds().size());
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/beeline; /usr/bin/mvn test -Dsurefire.timeout=300)", StringUtils.join(cmds.getCmds().get(0).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/cli; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest.excludes.additional=**/TestCliDriverMethods)", StringUtils.join(cmds.getCmds().get(1).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/standalone-metastore; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestRetriesInRetryingHMSHandler,TestRetryingHMSHandler,TestSetUGIOnBothClientServer,TestSetUGIOnOnlyClient,TestSetUGIOnOnlyServer,TestStats,TestMetastoreSchemaTool,TestSchemaToolForMetastore,TestTxnHandlerNegative,TestTxnUtils -Dtest.groups=\"\")", StringUtils.join(cmds.getCmds().get(2).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/standalone-metastore; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestHdfsUtils,TestMetaStoreUtils -Dtest.groups=\"\")", StringUtils.join(cmds.getCmds().get(3).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/ql; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestCleaner2)", StringUtils.join(cmds.getCmds().get(4).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/ql; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=CompactorTest,TestCleaner,TestInitiator,TestWorker2)", StringUtils.join(cmds.getCmds().get(5).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/itests/qtest; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestContribCliDriver -DskipSparkTests)", StringUtils.join(cmds.getCmds().get(6).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/itests/qtest; USER=dtestuser /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestMiniLlapLocalCliDriver -DskipSparkTests -Dqfile=llapdecider.q,acid_bucket_pruning.q,insert_into1.q,bucket6.q)", StringUtils.join(cmds.getCmds().get(7).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/itests/qtest; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestCliDriver -DskipSparkTests -Dqfile=authorization_show_grant.q)", StringUtils.join(cmds.getCmds().get(8).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/itests/qtest; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestCliDriver -DskipSparkTests -Dqfile=masking_acid_no_masking.q,masking_8.q,masking_9.q,masking_6.q)", StringUtils.join(cmds.getCmds().get(9).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/itests/qtest; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestCliDriver -DskipSparkTests -Dqfile=masking_7.q)", StringUtils.join(cmds.getCmds().get(10).shellCommand(), " "));
  }

  @Test
  public void qFilesFromProperties() {
    HiveContainerCommandFactory cmds = new HiveContainerCommandFactory();
    Properties testProperties = new Properties();
    testProperties.setProperty("encrypted.query.files", "encryption_join_unencrypted_tbl.q,encryption_insert_partition_static.q, encryption_insert_values.q encryption_drop_view.q ");
    testProperties.setProperty("beeline.positive.include", "drop_with_concurrency.q,escape_comments.q");
    testProperties.setProperty("minimr.query.negative.files", "cluster_tasklog_retrieval.q,file_with_header_footer_negative.q,local_mapred_error_cache.q,mapreduce_stack_trace.q");

    Set<String> qfiles = cmds.testTestProperties(testProperties, "encrypted.query.files");
    Assert.assertEquals(4, qfiles.size());
    Assert.assertTrue(qfiles.contains("encryption_join_unencrypted_tbl.q"));
    Assert.assertTrue(qfiles.contains("encryption_insert_partition_static.q"));
    Assert.assertTrue(qfiles.contains("encryption_insert_values.q"));
    Assert.assertTrue(qfiles.contains("encryption_drop_view.q"));

    qfiles = cmds.testTestProperties(testProperties, "beeline.positive.include", "minimr.query.negative.files");
    Assert.assertEquals(6, qfiles.size());
    Assert.assertTrue(qfiles.contains("drop_with_concurrency.q"));
    Assert.assertTrue(qfiles.contains("escape_comments.q"));
    Assert.assertTrue(qfiles.contains("cluster_tasklog_retrieval.q"));
    Assert.assertTrue(qfiles.contains("file_with_header_footer_negative.q"));
    Assert.assertTrue(qfiles.contains("local_mapred_error_cache.q"));
    Assert.assertTrue(qfiles.contains("mapreduce_stack_trace.q"));
  }

  private static class TestContainerClient extends MockContainerClient {
    public TestContainerClient(String containerName, String cannedDir, File buildDir, int rc) throws IOException {
      super(containerName, cannedDir, buildDir, rc);
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd) {
      // Doing our own mocking here
      String shellCmd = StringUtils.join(cmd.shellCommand(), " ");
      if (shellCmd.contains("cat itests/src/test/resources/testconfiguration.properties")) {
        return new ContainerResult(cmd, containerName, 0, "minillap.query.files=acid_bucket_pruning.q,\\\n" +
            "  bucket6.q\n" +
            "minillap.shared.query.files=insert_into1.q,\\\n" +
            " llapdecider.q\n" +
            "minitez.query.files=acid_vectorization_original_tez.q,\\\n" +
            "  explainuser_3.q,\\\n" +
            "  explainanalyze_1.q");
      } else if (shellCmd.contains("standalone-metastore") && shellCmd.contains("find")) {
        return new ContainerResult(cmd, containerName, 0, "standalone-metastore/src/test/java//org/apache/hadoop/hive/metastore/TestRetriesInRetryingHMSHandler.java\n" +
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
        return new ContainerResult(cmd, containerName, 0, "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/CompactorTest.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestCleaner.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestCleaner2.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestInitiator.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestWorker.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestWorker2.java\n");
      } else if (shellCmd.contains("find") && shellCmd.contains("clientpositive")) {
        return new ContainerResult(cmd, containerName, 0, "ql/src/test/queries/clientpositive/authorization_show_grant.q\n" +
            "ql/src/test/queries/clientpositive/masking_5.q\n" +
            "ql/src/test/queries/clientpositive/masking_6.q\n" +
            "ql/src/test/queries/clientpositive/masking_7.q\n" +
            "ql/src/test/queries/clientpositive/masking_8.q\n" +
            "ql/src/test/queries/clientpositive/masking_9.q\n" +
            "ql/src/test/queries/clientpositive/masking_acid_no_masking.q\n" +
            "ql/src/test/queries/clientpositive/acid_vectorization_original_tez.q\n" +
            "ql/src/test/queries/clientpositive/explainuser_3.q\n" +
            "ql/src/test/queries/clientpositive/explainanalyze_1.q\n");
      } else {
        throw new RuntimeException("Unexpected cmd " + shellCmd);
      }
    }
  }
}
