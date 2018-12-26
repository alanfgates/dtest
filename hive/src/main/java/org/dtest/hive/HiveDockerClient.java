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

import org.dtest.core.docker.DockerContainerClient;
import org.dtest.core.impl.ProcessResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Matcher;

public class HiveDockerClient extends DockerContainerClient {
  private static final Logger LOG = LoggerFactory.getLogger(HiveDockerClient.class);

  @Override
  protected void checkBuildSucceeded(ProcessResults res) throws IOException {
    Matcher m = IMAGE_SUCCESS.matcher(res.stdout);
    // We should see "BUILD SUCCESS" twice, once for the main build and once for itests
    if (res.rc != 0 || !(m.find() && m.find())) {
      // We might have read some from cache, check that before bailing
      m = USING_CACHE.matcher(res.stdout);
      if (res.rc != 0 || !(m.find() && m.find())) {
        // We might have read some from cache, check that before bailing
        throw new IOException("Failed to build image, see logs for error message: " + res.stderr);
      }
    }
  }

}
