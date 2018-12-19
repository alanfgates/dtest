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
package org.dtest.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "dtest",
      defaultPhase = LifecyclePhase.TEST,
      requiresDirectInvocation = true)
public class DtestPlugin extends AbstractMojo {

  /**
   * Default repository to use in the build.  This must be set.  You can override this with values specific
   */
  @Parameter
  String repo;

  // Need parameters for
  // repo - master repo must be set
  // branch - default to what makes sense for the CodeRepository
  // label - auto generates something
  // base dir = default to target
  // Need options for where properties and yaml files are, default to resources
  // How do I get a hold of properties already set, rather than having a parameter for every config value?
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    // Move dtest.properties and dtest.yaml into target/conf

    // Set any passed in values in properties

    // build the config object

    // preapre the build

    // run the build

  }



}
