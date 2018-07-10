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

import org.apache.hive.testutils.dtest.impl.DTestLogger;
import org.apache.hive.testutils.dtest.hive.HiveContainerCommandFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class TestContainerCommandFactory {

  public static class DummyContainerCommandFactory extends ContainerCommandFactory {
    @Override
    public List<ContainerCommand> getContainerCommands(ContainerClient containerClient,
                                                       BuildInfo label,
                                                       DTestLogger logger)
        throws IOException {
      return Collections.emptyList();
    }
  }

  @Test
  public void defaultFactory() throws IOException {
    Config.CONTAINER_COMMAND_FACTORY.unset();
    ContainerCommandFactory factory = ContainerCommandFactory.get();
    Assert.assertEquals(HiveContainerCommandFactory.class, factory.getClass());
  }

  @Test
  public void specifiedFactory() throws IOException {
    Config.CONTAINER_COMMAND_FACTORY.set(DummyContainerCommandFactory.class.getName());
    ContainerCommandFactory factory = ContainerCommandFactory.get();
    Assert.assertEquals(DummyContainerCommandFactory.class, factory.getClass());
  }
}
