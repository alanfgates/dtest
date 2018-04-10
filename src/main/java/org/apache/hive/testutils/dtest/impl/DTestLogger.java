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
package org.apache.hive.testutils.dtest.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DTestLogger {
  private static final String LOG_FILE = "dtest.log";

  private static Writer writer;
  private static Thread writerThread;
  private static BlockingQueue<Message> queue;

  /**
   * Constructor for your main program, this one will open a new log file.
   * @param dir directory where output log will be written
   * @throws IOException the log cannot be opened or written to.  If you get this you should give
   * up as nothing will be logged.
   */
  public DTestLogger(String dir) throws IOException {
    if (writerThread != null) {
      throw new IOException("Attempt to create another writer when one already exists!");
    }
    queue = new LinkedBlockingQueue<>();
    writer = new Writer(dir);
    writerThread = new Thread(writer);
    writerThread.setDaemon(true);
    writerThread.start();
  }

  public DTestLogger() throws IOException {
    if (writerThread == null) {
      throw new IOException("Attempt to create test log when it has not yet been initialized.");
    }
  }

  /**
   * Write a message to the log.  This only puts the message in the queue, a separate thread
   * writes it to the log later.
   * @param containerId unique id for this container
   * @param message message to be logged.
   */
  public void write(String containerId, String message) {
    queue.offer(new Message(System.currentTimeMillis(), containerId, message));
  }

  /**
   * Close the writer.  It cannot be reopened once this is done.
   * @throws IOException the closing message cannot be written to the log
   */
  public synchronized void close() throws IOException {
    writer.close();
    writerThread = null;
  }

  private static class Message {
    private final long time;
    private final String containerId;
    private final String content;

    private Message(long time, String containerId, String content) {
      this.time = time;
      this.containerId = containerId;
      this.content = content;
    }
  }

  private static class Writer implements Runnable {
    private final String dir;
    private FileWriter mainWriter;
    private Map<String, FileWriter> containerWriters;

    Writer(String dir) throws IOException {
      this.dir = dir;
      mainWriter = new FileWriter(dir + File.separatorChar + LOG_FILE);
      mainWriter.write("Test started at " + new Date().toString());
      containerWriters = new HashMap<>();
    }

    @Override
    public void run() {
      while (mainWriter != null) {
        Message m;
        Calendar cal = Calendar.getInstance();
        try {
          while ((m = queue.take()) != null) {
            FileWriter containerWriter = getContainerWriter(m.containerId);
            cal.setTimeInMillis(m.time);
            String msg = cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + ':' +
                cal.get(Calendar.SECOND) + ',' + cal.get(Calendar.MILLISECOND) + " [" +
                m.containerId + "] :" + m.content + '\n';
            mainWriter.write(msg);
            containerWriter.write(msg);
          }
        } catch (InterruptedException e) {
          // Assume this means we're supposed to quit.
          mainWriter = null;
        } catch (IOException ioe) {
          System.err.println("Log writer died, you won't get any results!");
          mainWriter = null;
        }
      }
    }

    void close() throws IOException {
      while (mainWriter != null && queue.size() > 0) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          // Assume we were interrupted, see if the queue is done.
        }
      }
      if (mainWriter != null) {
        mainWriter.write("Test completed at " + new Date().toString());
      }
      mainWriter = null;
    }

    private FileWriter getContainerWriter(String containerId) throws IOException {
      FileWriter writer = containerWriters.get(containerId);
      if (writer == null) {
        writer = new FileWriter(dir + File.separatorChar + containerId + ".log");
        containerWriters.put(containerId, writer);
      }
      return writer;
    }
  }


}
