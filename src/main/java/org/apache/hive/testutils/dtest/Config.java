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
import org.apache.hive.testutils.dtest.impl.DockerClientFactory;
import org.apache.hive.testutils.dtest.impl.MvnCommandFactory;
import org.apache.hive.testutils.dtest.impl.SimpleAnalyzerFactory;
import org.apache.hive.testutils.dtest.impl.TimeInterval;
import org.apache.hive.testutils.dtest.impl.Utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public enum Config {

  // Implementation of ContainerClientFactory
  CONTAINER_CLIENT_FACTORY("dtest.container.client.factory", DockerClientFactory.class),
  // Implementation of ContainerCommandFactory
  CONTAINER_COMMAND_FACTORY("dtest.container.command.factory", MvnCommandFactory.class),
  // Maximum amount of time to wait for container to run
  CONTAINER_RUN_TIME("dtest.container.run.time", new TimeInterval(3, TimeUnit.HOURS)),
  // Maximum amount of time to wait for image to build
  IMAGE_BUILD_TIME("dtest.image.build.time", new TimeInterval(30, TimeUnit.MINUTES)),
  // Implementation of ResultAnalyzerFactory
  RESULT_ANALYZER_FACTORY("dtest.result.analyzer.factory", SimpleAnalyzerFactory.class),
  // Maximum amount of time to wait for a test to run
  TEST_RUN_TIME("dtest.test.run.time", new TimeInterval(90, TimeUnit.MINUTES)),
  // Number of tests to run per container
  TESTS_PER_CONTAINER("dtest.tests.per.container", 10);


  private final String property;
  private final Object defaultValue;
  private Object value;

  Config(String property, Object defaultValue) {
    this.property = property;
    this.defaultValue = defaultValue;
  }

  public <T> Class<? extends T> getAsClass(Class<T> clazz) throws IOException {
    assert defaultValue.getClass().isInstance(clazz);
    if (value == null) {
      String className = System.getProperty(property);
      if (nullOrEmtpy(className)) {
        value = defaultValue;
      } else {
        value = Utils.getClass(className, clazz);
      }
    }
    return (Class<? extends T>)value;
  }

  public int getAsInt() {
    assert defaultValue.getClass() == Integer.class;
    if (value == null) {
      String str = System.getProperty(property);
      if (nullOrEmtpy(str)) value = defaultValue;
      else value = Integer.valueOf(str);
    }
    return (int)value;
  }

  public long getAsSeconds() {
    assert defaultValue.getClass() == TimeInterval.class;
    if (value == null) {
      String timeUnitName = System.getProperty(property + ".unit");
      String durationStr = System.getProperty(property);
      if (nullOrEmtpy(timeUnitName) || nullOrEmtpy(durationStr)) {
        value = defaultValue;
      } else {
        value = new TimeInterval(Long.valueOf(durationStr), TimeUnit.valueOf(timeUnitName));
      }
    }
    return TimeUnit.SECONDS.convert(((TimeInterval)value).duration, ((TimeInterval)value).unit);
  }

  public String getAsString() {
    assert defaultValue.getClass() == String.class;
    if (value == null) {
      value = System.getProperty(property);
      if (nullOrEmtpy((String)value)) value = defaultValue;
    }
    return value.toString();
  }

  @VisibleForTesting
  public void set(Class<?> clazz) {
    value = null;
    System.setProperty(property, clazz.getName());
  }

  @VisibleForTesting
  public void unset() {
    value = null;
    System.setProperty(property, "");
  }

  private boolean nullOrEmtpy(String x) {
    return x == null || x.isEmpty();
  }



}
