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
package org.dtest.core.impl;

import org.dtest.core.Config;
import org.dtest.core.testutils.TestUtilities;
import org.junit.Assert;
import org.junit.Test;

public class TestCommandFinder {

  @Test
  public void findInDefaultPath() {
    CommandFinder.forceReset();
    Config cfg = TestUtilities.buildCfg();
    Assert.assertEquals("/bin/ls", CommandFinder.get(cfg).findCommand("ls"));
    Assert.assertNull(CommandFinder.get(cfg).findCommand("nosuch"));
    Assert.assertNull(CommandFinder.get(cfg).findCommand("dtest"));
  }

  @Test
  public void findInExtraPath() {
    CommandFinder.forceReset();
    String scriptDir = System.getProperty("basedir") + "/src/main/scripts";
    Config cfg = TestUtilities.buildCfg(CommandFinder.CFG_COMMANDFINDER_ADDITIONALPATH, "/etc:" + scriptDir);
    Assert.assertEquals(scriptDir + "/dtest", CommandFinder.get(cfg).findCommand("dtest"));
    Assert.assertEquals("/bin/ls", CommandFinder.get(cfg).findCommand("ls"));
    Assert.assertNull(CommandFinder.get(cfg).findCommand("nosuch"));
  }
}
