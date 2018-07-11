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

import org.dtest.core.impl.Utils;

import java.io.IOException;

/**
 * A class to build ContainerClients.  If you want to build your own ContainerClient implement an
 * instance of this that returns that type of client.
 */
public abstract class ContainerClientFactory {

  public static ContainerClientFactory get() throws IOException {
    Class<? extends ContainerClientFactory> clazz =
        Config.CONTAINER_CLIENT_FACTORY.getAsClass(ContainerClientFactory.class);
    return Utils.newInstance(clazz);
  }

  /**
   * Get the client.
   * @param buildInfo information for this build
   * @return client
   */
  public abstract ContainerClient getClient(BuildInfo buildInfo);
}
