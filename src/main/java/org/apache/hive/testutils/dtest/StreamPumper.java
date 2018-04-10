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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamPumper implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(StreamPumper.class);

  private final AtomicBoolean keepGoing;
  private final BufferedReader reader;
  private final StringBuilder buffer;

  public StreamPumper(AtomicBoolean keepGoing, InputStream input) {
    this.keepGoing = keepGoing;
    reader = new BufferedReader(new InputStreamReader(input));
    buffer = new StringBuilder();
  }

  public String getOutput() {
    return buffer.toString();
  }

  @Override
  public void run() {
    try {
      while (keepGoing.get()) {
        if (reader.ready()) {
          String s = reader.readLine();
          LOG.info(s);
          buffer.append(s).append('\n');
        } else {
          Thread.sleep(1000);
        }
      }
    } catch (Exception e) {
      LOG.error("Caught exception while puming stream", e);
    }

  }
}
