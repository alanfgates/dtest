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

import com.google.common.annotations.VisibleForTesting;
import org.apache.hive.testutils.dtest.hive.HiveDockerClientFactory;
import org.apache.hive.testutils.dtest.simple.SimpleResultAnalyzerFactory;
import org.apache.hive.testutils.dtest.impl.TimeInterval;
import org.apache.hive.testutils.dtest.impl.Utils;
import org.apache.hive.testutils.dtest.hive.HiveContainerCommandFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.apache.hive.testutils.dtest.DockerTest.DTEST_HOME;

public enum Config {
  // Implementation of ContainerClientFactory
  CONTAINER_CLIENT_FACTORY("dtest.container.client.factory", HiveDockerClientFactory.class.getName()),
  // Implementation of ContainerCommandFactory
  CONTAINER_COMMAND_FACTORY("dtest.container.command.factory", HiveContainerCommandFactory.class.getName()),
  // Maximum amount of time to wait for container to run
  CONTAINER_RUN_TIME("dtest.container.run.time", "3", TimeUnit.HOURS.name()),
  // Maximum amount of time to wait for image to build
  IMAGE_BUILD_TIME("dtest.image.build.time", "30", TimeUnit.MINUTES.name()),
  // Implementation of ResultAnalyzerFactory
  RESULT_ANALYZER_FACTORY("dtest.result.analyzer.factory", SimpleResultAnalyzerFactory.class.getName()),
  // Maximum amount of time to wait for a test to run
  TEST_RUN_TIME("dtest.test.run.time", "90", TimeUnit.MINUTES.name()),
  // Number of tests to run per container
  TESTS_PER_CONTAINER("dtest.tests.per.container", "10");

  @VisibleForTesting
  static final String CONF_DIR = "conf";
  @VisibleForTesting
  static final String PROPERTIES_FILE = "dtest.properties";

  private final String property;
  private final String defaultValue;
  private final String defaultTimeUnit;
  private Object value;

  Config(String property, String defaultValue) {
    this(property, defaultValue, null);
  }

  Config(String property, String defaultValue, String defaultTimeUnit) {
    this.property = property;
    this.defaultValue = defaultValue;
    this.defaultTimeUnit = defaultTimeUnit;
  }

  public <T> Class<? extends T> getAsClass(Class<T> clazz) throws IOException {
    if (value == null) {
      String className = System.getProperty(property, defaultValue);
      value = Utils.getClass(className, clazz);
    }
    return (Class<? extends T>)value;
  }

  public int getAsInt() {
    if (value == null) {
      value = Integer.valueOf(System.getProperty(property, defaultValue));
    }
    return (int)value;
  }

  public long getAsTime(TimeUnit unit) {
    if (value == null) {
      String timeUnitName = System.getProperty(property + ".unit", defaultTimeUnit);
      String durationStr = System.getProperty(property, defaultValue);
      value = new TimeInterval(Long.valueOf(durationStr), TimeUnit.valueOf(timeUnitName));
    }
    return unit.convert(((TimeInterval)value).duration, ((TimeInterval)value).unit);

  }

  public String getAsString() {
    if (value == null) {
      value = System.getProperty(property, defaultValue);
    }
    return value.toString();
  }

  @VisibleForTesting
  void set(String newVal) {
    value = null;
    System.setProperty(property, newVal);
  }

  @VisibleForTesting
  String getProperty() {
    return property;
  }

  @VisibleForTesting
  void resetValue() {
    value = null;
  }

  @VisibleForTesting
  void unset() {
    value = null;
    System.getProperties().remove(property);
  }

  /**
   * Read the configuration file and set the system properties based on values in the file.  This
   * method expects the configuration file to be in $DTEST_HOME/conf/dtest.properties.
   * @throws IOException If the file cannot be found or is not readable or is not the proper format.
   */
  public static void fromConfigFile() throws IOException {
    String dtestHome = System.getenv(DTEST_HOME);
    if (dtestHome == null || dtestHome.isEmpty()) {
      throw new IOException("Unable to find configuration file, please set DTEST_HOME");
    }
    String filename = dtestHome + File.separator + CONF_DIR + File.separator + PROPERTIES_FILE;
    FileInputStream input = new FileInputStream(filename);
    System.getProperties().load(input);
    input.close();

  }

}
