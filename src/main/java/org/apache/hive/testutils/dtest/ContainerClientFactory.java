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

/**
 * A class to build ContainerClients.  If you want to build your own ContainerClient implement an
 * instance of this that returns that type of client.
 */
public abstract class ContainerClientFactory {

  static ContainerClientFactory get(String factoryClassName) throws IOException {
    if (factoryClassName == null) factoryClassName = DockerClientFactory.class.getName();

    Class<? extends ContainerClientFactory> clazz = Utils.getClass(factoryClassName,
        ContainerClientFactory.class);
    return Utils.newInstance(clazz);
  }

  /**
   * Get the client.
   * @param buildNum Number of this build.
   * @return client
   * @throws IOException unable to instantiate the client.
   */
  public abstract ContainerClient getClient(int buildNum);
}
