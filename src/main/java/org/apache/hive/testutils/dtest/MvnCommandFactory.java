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
package org.apache.hive.testutils.dtest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MvnCommandFactory extends ContainerCommandFactory {

  @Override
  public List<ContainerCommand> getContainerCommands(String baseDir) throws IOException {
    List<ContainerCommand> cmds = new ArrayList<>();
    // TODO This is turbo brittle.  It should be scanning the source for pom files and adding a
    // command for each, and then counting qfiles and dividing them up.

    // Unit tests
    cmds.add(new MvnCommand(baseDir, "itests/hive-unit"));
    cmds.add(new MvnCommand(baseDir, "accumulo-handler"));
    cmds.add(new MvnCommand(baseDir, "beeline"));
    cmds.add(new MvnCommand(baseDir, "cli"));
    cmds.add(new MvnCommand(baseDir, "common"));
    cmds.add(new MvnCommand(baseDir, "hplsql"));
    cmds.add(new MvnCommand(baseDir, "jdbc"));
    cmds.add(new MvnCommand(baseDir, "jdbc-handler"));
    cmds.add(new MvnCommand(baseDir, "serde"));
    cmds.add(new MvnCommand(baseDir, "shims"));
    cmds.add(new MvnCommand(baseDir, "storage-api"));
    cmds.add(new MvnCommand(baseDir, "llap-client"));
    cmds.add(new MvnCommand(baseDir, "llap-common"));
    cmds.add(new MvnCommand(baseDir, "llap-server"));
    cmds.add(new MvnCommand(baseDir, "standalone-metastore").addProperty("test.groups", ""));
    cmds.add(new MvnCommand(baseDir, "druid-handler"));
    cmds.add(new MvnCommand(baseDir, "service"));
    cmds.add(new MvnCommand(baseDir, "spark-client"));
    cmds.add(new MvnCommand(baseDir, "hbase-handler"));
    cmds.add(new MvnCommand(baseDir, "hcatalog/core"));
    cmds.add(new MvnCommand(baseDir, "hcatalog/hcatalog-pig-adapter"));
    cmds.add(new MvnCommand(baseDir, "hcatalog/server-extensions"));
    cmds.add(new MvnCommand(baseDir, "hcatalog/streaming"));
    cmds.add(new MvnCommand(baseDir, "hcatalog/webhcat/java-client"));
    cmds.add(new MvnCommand(baseDir, "hcatalog/webhcat/svr"));
    cmds.add(new MvnCommand(baseDir, "ql"));

    // itests junit tests
    cmds.add(new MvnCommand(baseDir, "itests/hcatalog-unit").addExclude("TestSequenceFileReadWrite"));
    cmds.add(new MvnCommand(baseDir, "itests/hive-blobstore"));
    cmds.add(new MvnCommand(baseDir, "itests/hive-minikdc"));
    cmds.add(new MvnCommand(baseDir, "itests/hive-unit-hadoop2"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest-accumulo"));

    // qfile tests
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestBeeLineDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCompareCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestContribCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestContribNegativeCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestEncryptedHDFSCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestHBaseCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestHBaseNegativeCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestMiniDruidCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestMiniLlapCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestMiniLlapLocalCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeMinimrCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestParseNegativeDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestMinimrCliDriver"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestMiniTezCliDriver"));

    // Super big qfile tests broken out
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("v.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("a[a-t].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("au.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("a[v-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("b.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("c[a-n].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("co.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("c[p-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("d[a-l].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("d[m-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("e.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("[fhkn].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("g.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("i.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("j.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("l.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("m.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("[oq].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("pa.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("p[b-e].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("p[f-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("[rw-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("s[a-d].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("s[e-l].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("s[m-s].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("s[t-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("t.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("u[a-d].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestCliDriver").setqFilePattern("u[e-z].\\*"));

    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("a[a-t].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("a[u-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("[bd].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("c.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("[e-h].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("i.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("[j-o].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("[p-rtv-z].\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("s.\\*"));
    cmds.add(new MvnCommand(baseDir, "itests/qtest").setTest("TestNegativeCliDriver").setqFilePattern("u.\\*"));

    return cmds;
  }
}
