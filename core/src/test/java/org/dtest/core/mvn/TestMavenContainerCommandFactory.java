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
package org.dtest.core.mvn;

import org.apache.commons.lang3.StringUtils;
import org.dtest.core.BuildInfo;
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

public class TestMavenContainerCommandFactory {

  @Test
  public void buildCommands() throws IOException {
    File buildDir = TestUtilities.createBuildDir();
    Config cfg = TestUtilities.buildCfg(BuildInfo.CFG_BUILDINFO_LABEL, "profile",
                                    BuildInfo.CFG_BUILDINFO_BASEDIR, System.getProperty("java.io.tmpdir"));
    TestLogger log = new TestLogger();
    MavenContainerCommandFactory cmds = new MavenContainerCommandFactory();
    cmds.setConfig(cfg);
    cmds.setLog(log);
    BuildInfo buildInfo = new BuildInfo(TestUtilities.buildYaml(cfg, log), new GitSource(), true, "1");
    buildInfo.setConfig(cfg).setLog(log);
    buildInfo.getBuildDir();
    cmds.buildContainerCommands(new TestContainerClient("test-maven-container-command-factory", "allgood", buildDir, 0), buildInfo);
    Assert.assertEquals(7, cmds.getCmds().size());
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/beeline; /usr/bin/mvn test -Dsurefire.timeout=300)", StringUtils.join(cmds.getCmds().get(0).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/cli; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest.excludes.additional=**/TestCliDriverMethods)", StringUtils.join(cmds.getCmds().get(1).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/standalone-metastore; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestRetriesInRetryingHMSHandler,TestRetryingHMSHandler,TestSetUGIOnBothClientServer,TestSetUGIOnOnlyClient,TestSetUGIOnOnlyServer,TestStats,TestMetastoreSchemaTool,TestSchemaToolForMetastore,TestTxnHandlerNegative,TestTxnUtils -Dtest.groups=\"\")", StringUtils.join(cmds.getCmds().get(2).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/standalone-metastore; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestHdfsUtils,TestMetaStoreUtils -Dtest.groups=\"\")", StringUtils.join(cmds.getCmds().get(3).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/ql; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestCleaner2)", StringUtils.join(cmds.getCmds().get(4).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/ql; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=CompactorTest,TestCleaner,TestInitiator,TestWorker2)", StringUtils.join(cmds.getCmds().get(5).shellCommand(), " "));
    Assert.assertEquals("/bin/bash -c ( cd " + buildDir + "/itests/qtest; /usr/bin/mvn test -Dsurefire.timeout=300 -Dtest=TestContribCliDriver -DskipSparkTests)", StringUtils.join(cmds.getCmds().get(6).shellCommand(), " "));
    log.dumpToLog();
  }

  private static class TestContainerClient extends MockContainerClient {

    public TestContainerClient(String containerName, String cannedDir, File buildDir, int rc) throws IOException {
      super(containerName, cannedDir, buildDir, rc);
    }

    @Override
    public ContainerResult runContainer(ContainerCommand cmd) {
      // Doing our own mocking here
      String shellCmd = StringUtils.join(cmd.shellCommand(), " ");
      if (shellCmd.contains("standalone-metastore") && shellCmd.contains("find")) {
        return new ContainerResult(cmd, "unnamed", 0, "standalone-metastore/src/test/java//org/apache/hadoop/hive/metastore/TestRetriesInRetryingHMSHandler.java\n" +
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
        return new ContainerResult(cmd, "unnamed", 0, "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/CompactorTest.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestCleaner.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestCleaner2.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestInitiator.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestWorker.java\n" +
            "ql/src/test/org/apache/hadoop/hive/ql/txn//compactor/TestWorker2.java\n");
      } else {
        throw new RuntimeException("Unexpected cmd " + shellCmd);
      }
    }
  }

}
