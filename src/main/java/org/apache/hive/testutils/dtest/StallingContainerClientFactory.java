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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class StallingContainerClientFactory extends ContainerClientFactory {
  private static final Logger LOG = LoggerFactory.getLogger(StallingContainerClientFactory.class);

  @Override
  public ContainerClient getClient(String label) {
    return new ContainerClient() {
      @Override
      public void defineImage(String dir, String repo, String branch, String label) throws IOException {

      }

      @Override
      public void buildImage(String dir, long toWait, TimeUnit unit, DTestLogger logger) throws
          IOException {
        LOG.debug("Would build an image, but think I'll sleep instead");

      }

      @Override
      public ContainerResult runContainer(long toWait, TimeUnit unit, ContainerCommand cmd,
                                          DTestLogger logger) throws IOException {
        // Sleep for half the max duration.
        LOG.info("Going to sleep for " + toWait + " " + unit.name());
        long milliseconds = unit.toMillis(toWait);
        try {
          Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
          throw new IOException("Interrupted ",e);
        }
        return new ContainerResult(cmd.containerName(), 0, "Slept for " + milliseconds +
            " milliseconds");
      }
    };
  }
}
