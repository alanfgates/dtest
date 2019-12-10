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
package org.dtest.core.testutils;

import org.dtest.core.DTestLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of DTestLogger that collects all of the log output so it can be examined by tests.  Results can,
 * and should, be dumped to Slf4j log by calling {@link #dumpToLog()} at the end of the test.
 */
public class TestLogger implements DTestLogger {
  private Logger log = LoggerFactory.getLogger(TestUtilities.class);
  private List<String> entries = new ArrayList<>();

  @Override
  public void error(String msg) {
    append("ERROR", msg);
  }

  @Override
  public void error(String msg, Throwable t) {
    append("ERROR", msg, t);
  }

  @Override
  public void warn(String msg) {
    append("WARN", msg);
  }

  @Override
  public void warn(String msg, Throwable t) {
    append("WARN", msg, t);
  }

  @Override
  public void info(String msg) {
    append("INFO", msg);
  }

  @Override
  public void info(String msg, Throwable t) {
    append("INFO", msg, t);
  }

  @Override
  public void debug(String msg) {
    append("DEBUG", msg);
  }

  @Override
  public void debug(String msg, Throwable t) {
    append("DEBUG", msg, t);
  }

  @Override
  public boolean isErrorEnabled() {
    return true;
  }

  @Override
  public boolean isWarnEnabled() {
    return true;
  }

  @Override
  public boolean isInfoEnabled() {
    return true;
  }

  @Override
  public boolean isDebugEnabled() {
    return true;
  }

  @Override
  public String toString() {
    // Returns the entire log in one big string, with each entry separated by returns
    StringBuilder buf = new StringBuilder();
    for (String entry : entries) buf.append(entry).append("\n");
    return buf.toString();
  }

  /**
   * Dump the contents of this log to the slf4j log, at info level.
   */
  public void dumpToLog() {
    log.info("Entire logs from test:");
    for (String entry : entries) log.info(entry);
  }

  private void append(String level, String msg) {
    append(level, msg, null);
  }

  private void append(String level, String msg, Throwable t) {
    entries.add(level + ": " + msg);
    if (t != null) {
      entries.add("Caught exception " + t.getClass().getName() + " with message " + t.getMessage());
      StackTraceElement[] stack = t.getStackTrace();
      for (StackTraceElement element : stack) entries.add("   " + element.toString());
    }

  }
}
