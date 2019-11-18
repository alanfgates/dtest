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
package org.dtest.ozone;

import org.dtest.core.mvn.MavenContainerCommand;
import org.dtest.core.mvn.MavenContainerCommandFactory;

import java.util.Collections;
import java.util.List;

public class OzoneContainerCommandFactory extends MavenContainerCommandFactory {

  @Override
  protected MavenContainerCommand buildCommand(String buildDir, int cmdNum) {
    return new OzoneContainerCommand(buildDir, cmdNum);
  }

  @Override
  public List<String> getInitialBuildCommand() {
    return Collections.singletonList("/usr/bin/mvn install -f pom.ozone.xml -DskipTests");
  }
}
