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
package org.dtest.core.impl;

import org.dtest.core.DTestLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamPumper implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(StreamPumper.class);

  private final AtomicBoolean keepGoing;
  private final BufferedReader reader;
  private final StringBuilder buffer;
  private final String containerId;
  private final DTestLogger logger;

  StreamPumper(AtomicBoolean keepGoing, InputStream input, String containerId,
               DTestLogger logger) {
    this.keepGoing = keepGoing;
    reader = new BufferedReader(new InputStreamReader(input));
    this.containerId = containerId;
    this.logger = logger;
    buffer = new StringBuilder();
  }

  String getOutput() {
    return buffer.toString();
  }

  @Override
  public void run() {
    try {
      while (keepGoing.get()) {
        if (reader.ready()) {
          String s = reader.readLine();
          logger.write(containerId, s);
          buffer.append(s).append('\n');
        } else {
          Thread.sleep(1000);
        }
      }
    } catch (Exception e) {
      LOG.error("Caught exception while pumping stream", e);
    }
  }

  /**
   * Run this after you've finished the process to make sure the last lines are collected.
   */
  public void finalPump() throws IOException {
    assert !keepGoing.get();
    while (reader.ready()) {
      String s = reader.readLine();
      logger.write(containerId, s);
      buffer.append(s).append('\n');
    }

  }
}
