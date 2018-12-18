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

@Mojo(name = "dtest",
      defaultPhase = LifecyclePhase.TEST,
      requiresDirectInvocation = true)
public class DtestPlugin extends AbstractMojo {
  // Need parameters for
  // CFG_BUILD_BASE_DIR - required
  // CFG_CODE_SOURCE_CLASS
  // CFG_CONTAINER_CLIENT
  // CFG_CONTAINER_RUN_TIME
  // CFG_IMAGE_BUILD_TIME
  // CFG_TEST_RUN_TIME
  // CFG_BASE_DIR
  // CFG_TESTS_PER_CONTAINER
  // CFG_CONTAINER_COMMAND_LIST
  // CFG_NUM_CONTAINERS
  // CFG_GIT_REPO
  // CFG_GIT_BRANCH - required
  // CFG_RESULT_ANALYZER
  // Need options for where properties and yaml files are, default to resources
  // Figure out what the resources directory is
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

  }



}
