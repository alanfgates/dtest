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

import org.dtest.core.mvn.MavenContainerCommandFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Ozone specialization of MavenContainerCommandFactory.  Ozone requires at least maven 3.6, and many images comes
 * with earlier versions.  This takes maven out of the required package list and instead inserts a command to
 * download maven from an Apache mirror.
 */
public class OzoneContainerCommandFactory extends MavenContainerCommandFactory {

  @Override
  public List<String> getAdditionalDockerBuildCommands() {
    return Collections.singletonList("RUN (cd /usr/share; \\\n" +
        "     wget http://apache.cs.utah.edu/maven/maven-3/3.6.2/binaries/apache-maven-3.6.2-bin.tar.gz; \\\n" +
        "     tar zxf apache-maven-3.6.2-bin.tar.gz; \\\n" +
        "     ln -s /usr/share/apache-maven-3.6.2/bin/mvn /usr/bin/mvn; \\\n" +
        "    )");
  }

  @Override
  public List<String> getRequiredPackages() {
    // Ozone requires a specific version of maven, so don't install it via package, we'll download and install that
    // version.
    List<String> ourList = new LinkedList<>(super.getRequiredPackages());
    ourList.removeIf(s -> s.equals("maven"));
    ourList.add("which");
    return ourList;
  }

}
