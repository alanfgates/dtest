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

import org.apache.hive.testutils.dtest.impl.ContainerResult;
import org.apache.hive.testutils.dtest.impl.DTestLogger;
import org.apache.hive.testutils.dtest.impl.DockerClientFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TestContainerClientFactory {

  public static class DummyContainerClientFactory extends ContainerClientFactory {
    @Override
    public ContainerClient getClient(String label) {
      return new ContainerClient() {
        @Override
        public void defineImage(String dir, String repo, String branch, String label) throws IOException {

        }

        @Override
        public String getContainerBaseDir() {
          return null;
        }

        @Override
        public ContainerResult runContainer(long toWait, ContainerCommand cmd, DTestLogger logger) throws IOException {
          return null;
        }
      };
    }
  }

  @Test
  public void defaultFactory() throws IOException {
    Config.CONTAINER_CLIENT_FACTORY.unset();
    ContainerClientFactory factory = ContainerClientFactory.get();
    Assert.assertEquals(DockerClientFactory.class, factory.getClass());
  }

  @Test
  public void specifiedFactory() throws IOException {
    Config.CONTAINER_CLIENT_FACTORY.set(DummyContainerClientFactory.class);
    ContainerClientFactory factory = ContainerClientFactory.get();
    Assert.assertEquals(DummyContainerClientFactory.class, factory.getClass());
  }
}
