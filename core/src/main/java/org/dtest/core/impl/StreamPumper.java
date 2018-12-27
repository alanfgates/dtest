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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class used to pull in the output of a stream.  This is useful for running processes that generate large
 * amounts of output that would overflow the buffer of a {@link Process}.  This is connected to a single stream
 * (usually stdout or stderr).  It is designed to be run in a separate thread so that it can continually read its
 * input stream and buffer up the contents without the main thread needing to loop.  The contents of the stream can
 * be fetched at any time.
 */
public class StreamPumper implements Runnable {

  private final AtomicBoolean keepGoing;
  private final BufferedReader reader;
  private final StringBuilder buffer;
  private final String containerId;
  private final DTestLogger log;

  /**
   *
   * @param keepGoing An AtomicBoolean shared between the calling thread and this class.  When the calling thread wishes
   *                  to terminate the pumping the stream it sets this value to false.  This should be done after
   *                  the process whose output is being pumped has terminated and before calling
   *                  {@link #finalPump()} to assure that all the output is collected.
   * @param input input stream to read.
   * @param containerId id of the container whose output is being pumped.  This value is used in the log.
   * @param log log object
   */
  StreamPumper(AtomicBoolean keepGoing, InputStream input, String containerId, DTestLogger log) {
    this.keepGoing = keepGoing;
    reader = new BufferedReader(new InputStreamReader(input));
    this.containerId = containerId;
    this.log = log;
    buffer = new StringBuilder();
  }

  /**
   * Get the result of the output.  This does not guarantee all output has been collected, it grabs whatever
   * is currently available.
   * @return output
   */
  String getOutput() {
    return buffer.toString();
  }

  @Override
  public void run() {
    try {
      while (keepGoing.get()) {
        if (reader.ready()) {
          String s = reader.readLine();
          log.debug(containerId, s);
          buffer.append(s).append('\n');
        } else {
          Thread.sleep(1000);
        }
      }
    } catch (Exception e) {
      log.error("Caught exception while pumping stream", e);
    }
  }

  /**
   * Run this after you've finished the process to make sure the last lines are collected.  It checks to make sure
   * the calling thread has told the stream to stop pumping.
   * @throws IOException if the read from the stream fails.
   */
  public void finalPump() throws IOException {
    assert !keepGoing.get();
    while (reader.ready()) {
      String s = reader.readLine();
      log.debug(containerId, s);
      buffer.append(s).append('\n');
    }

  }
}
