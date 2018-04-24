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

public class Config {
  // Implementation of ContainerClientFactory
  public static final String CONTAINER_CLIENT_FACTORY = "dtest.container.client.factory";
  // Implementation of ContainerCommandFactory
  public static final String CONTAINER_COMMAND_FACTORY = "dtest.container.command.factory";
  // Implementation of ResultAnalyzerFactory
  public static final String RESULT_ANALYZER_FACTORY = "dtest.result.analyzer.factory";
  // Maximum amount of time to wait for image to build
  public static final String IMAGE_BUILD_TIME = "dtest.image.build.time";
  // Unit for image build time
  public static final String IMAGE_BUILD_TIME_UNIT = "dtest.image.build.time.unit";
  // Maximum amount of time to wait for container to run
  public static final String CONTAINER_RUN_TIME = "dtest.container.run.time";
  // Unit for container run time
  public static final String CONTAINER_RUN_TIME_UNIT = "dtest.container.run.time.unit";
  // Maximum amount of time to wait for a test to run
  public static final String TEST_RUN_TIME = "dtest.test.run.time";
  // Unit for test run time
  public static final String TEST_RUN_TIME_UNIT = "dtest.test.run.time.unit";
  // Number of tests to run per container
  public static final String TESTS_PER_CONTAINER = "dtest.tests.per.container";
}