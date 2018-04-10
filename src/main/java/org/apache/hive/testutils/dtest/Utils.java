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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Utils {
  private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

  @SuppressWarnings(value = "unchecked")
  public static <T> Class<? extends T> getClass(String className, Class<T> clazz)
      throws IOException {
    try {
      return (Class<? extends T>) Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new IOException("Failed to find class " + className, e);
    }
  }

  public static <T> T newInstance(Class<T> theClass) throws IOException {
    try {
      return theClass.newInstance();
    } catch (InstantiationException|IllegalAccessException e) {
      throw new IOException("Unable to instantiate " + theClass.getName(), e);
    }
  }

  static ProcessResults runProcess(long secondsToWait, String... cmd) throws IOException {
    LOG.info("Going to run: " + StringUtils.join(cmd, " ") + "\n");
    Process proc = Runtime.getRuntime().exec(cmd);
    AtomicBoolean running = new AtomicBoolean(true);
    StreamPumper stdout = new StreamPumper(running, proc.getInputStream());
    StreamPumper stderr = new StreamPumper(running, proc.getErrorStream());
    new Thread(stdout).start();
    new Thread(stderr).start();
    try {
      if (!proc.waitFor(secondsToWait, TimeUnit.SECONDS)) {
        throw new RuntimeException("Process " + cmd[0] + " failed to run in " + secondsToWait +
            " seconds");
      }
    } catch (InterruptedException e) {
      throw new IOException(e);
    } finally {
      running.set(false);
    }
    return new ProcessResults(stdout.getOutput(), stderr.getOutput(), proc.exitValue());
  }
}
