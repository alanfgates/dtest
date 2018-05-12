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

import com.google.common.annotations.VisibleForTesting;
import org.apache.hive.testutils.dtest.ResultAnalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@VisibleForTesting
public class SimpleResultAnalyzer implements ResultAnalyzer {
  private boolean hadTimeouts;
  private boolean runSucceeded;
  private AtomicInteger succeeded;
  private List<String> failed;
  private List<String> errors;
  private Map<String, Set<String>> logfiles;
  private final Pattern successLine;
  private final Pattern errorLine;
  private final Pattern unitTestError;
  private final Pattern unitTestFailure;
  private final Pattern qTestError;
  private final Pattern qTestFailure;
  private final Pattern timeout;

  @VisibleForTesting
  public SimpleResultAnalyzer() {
    // Access to these does not need to be synchronized because they only go from start state to
    // the opposite state (eg hadTimeouts starts at false and can move to true, but can never
    // move back to false).
    hadTimeouts = false;
    runSucceeded = true;
    // Access to these needs to be synchronized.
    succeeded = new AtomicInteger(0);
    failed = new Vector<>();
    errors = new Vector<>();
    logfiles = new ConcurrentHashMap<>(); // Access inside a key need not be synchronized as a
    // test is only executed inside a single container.
    successLine =
        Pattern.compile("\\[INFO\\] Tests run: ([0-9]+), Failures: ([0-9]+), Errors: ([0-9]+).*Time elapsed:.*");
    errorLine =
        Pattern.compile("\\[ERROR\\] Tests run: ([0-9]+), Failures: ([0-9]+), Errors: ([0-9]+).*Time elapsed:.*");
    unitTestError =
        Pattern.compile("\\[ERROR\\] ([A-Za-z0-9_]+).*\\.(Test[A-Za-z0-9_]+).*ERROR!");
    unitTestFailure =
        Pattern.compile("\\[ERROR\\] ([A-Za-z0-9_]+).*\\.(Test[A-Za-z0-9_]+).*FAILURE!");
    qTestFailure =
        Pattern.compile("\\[ERROR\\] testCliDriver\\[([A-Za-z0-9_]+)\\].*\\.(Test[A-Za-z0-9_]+).*FAILURE!");
    qTestError =
        Pattern.compile("\\[ERROR\\] testCliDriver\\[([A-Za-z0-9_]+)\\].*\\.(Test[A-Za-z0-9_]+).*ERROR!");
    timeout =
        Pattern.compile("\\[ERROR\\] Failed to execute goal .* There was a timeout or other error in the fork.*");
  }

  @Override
  public int getSucceeded() {
    return succeeded.get();
  }

  @Override
  public List<String> getFailed() {
    Collections.sort(failed);
    return failed;
  }

  @Override
  public List<String> getErrors() {
    Collections.sort(errors);
    return errors;
  }

  @Override
  public Map<String, Set<String>> logFilesToFetch() {
    return logfiles;
  }

  @Override
  public ContainerStatus analyzeLog(ContainerResult result) {
    String[] lines = result.logs.split("\n");
    boolean sawTimeout = false;
    for (String line : lines) sawTimeout |= analyzeLogLine(result, line);
    if (sawTimeout) {
      return ContainerStatus.TIMED_OUT;
    } else if (result.rc < 0 ||result.rc > 1) {
      runSucceeded = false;
      return ContainerStatus.FAILED;
    } else {
      return ContainerStatus.SUCCEEDED;
    }
  }

  @Override
  public boolean hadTimeouts() {
    return hadTimeouts;
  }

  @Override
  public boolean runSucceeded() {
    return runSucceeded;
  }

  // Returns true if it sees a timeout
  private boolean analyzeLogLine(ContainerResult result, String line) {
    count(line, successLine);
    count(line, errorLine);
    // Look first to see if it matches the qtest pattern, if not use the more general pattern.
    if (!findErrorsAndFailures(result, line, qTestError, qTestFailure)) {
      // Ok, now see if it matches the unit test pattern
      if (!findErrorsAndFailures(result, line, unitTestError, unitTestFailure)) {
        // Finally, look for timeouts
        Matcher m = timeout.matcher(line);
        if (m.matches()) {
          hadTimeouts = true;
          return true;
        }
      }
    }
    return false;
  }

  private void count(String line, Pattern pattern) {
    Matcher m = pattern.matcher(line);
    if (m.matches()) {
      int total = Integer.parseInt(m.group(1));
      int failures = Integer.parseInt(m.group(2));
      int errors = Integer.parseInt(m.group(3));
      succeeded.addAndGet(total - failures - errors);
    }
  }

  private boolean findErrorsAndFailures(ContainerResult result, String line, Pattern error,
                                        Pattern failure) {
    Matcher errorLine = error.matcher(line);
    if (errorLine.matches()) {
      errors.add(errorLine.group(2) + "." + errorLine.group(1));
      findLogFiles(result, line, errorLine.group(2));
      return true;
    } else {
      Matcher failureLine = failure.matcher(line);
      if (failureLine.matches()) {
        failed.add(failureLine.group(2) + "." + failureLine.group(1));
        findLogFiles(result, line, failureLine.group(2));
        return true;
      }
    }
    return false;
  }

  private void findLogFiles(ContainerResult result, String line, String testName) {
    Set<String> files = logfiles.computeIfAbsent(result.name, k -> new HashSet<>());
    Pattern p = Pattern.compile(".*(org\\.apache\\..*\\." + testName + ").*");
    Matcher m = p.matcher(line);
    if (!m.matches()) {
      throw new RuntimeException("Failed to find the full name of the failed test.");
    }
    files.add(result.containerDirectory + "/target/tmp/log/hive.log");
    files.add(result.containerDirectory + "/target/surefire-reports/" + m.group(1) + ".txt");
    files.add(result.containerDirectory + "/target/surefire-reports/" + m.group(1) + "-output.txt");
  }
}
