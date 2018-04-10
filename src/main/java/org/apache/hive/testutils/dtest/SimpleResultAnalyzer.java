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

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SimpleResultAnalyzer implements ResultAnalyzer {
  private boolean hadTimeouts;
  private boolean runSucceeded;
  private AtomicInteger succeeded;
  private List<String> failed;
  private List<String> errors;
  private final Pattern successLine;
  private final Pattern errorLine;
  private final Pattern unitTestError;
  private final Pattern unitTestFailure;
  private final Pattern qTestError;
  private final Pattern qTestFailure;
  private final Pattern timeout;

  SimpleResultAnalyzer() {
    // Access to these does not need to be synchronized because they only go from start state to
    // the opposite state (eg hadTimeouts starts at false and can move to true, but can never
    // move back to false).
    hadTimeouts = false;
    runSucceeded = true;
    // Access to these needs to be synchronized.
    succeeded = new AtomicInteger(0);
    failed = new Vector<>();
    errors = new Vector<>();
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
  public void analyzeLog(ContainerResult result) {
    String[] lines = result.logs.split("\n");
    for (String line : lines) analyzeLogLine(result, line);
    if (result.rc < 0 ||result.rc > 1) runSucceeded = false;
  }

  @Override
  public boolean hadTimeouts() {
    return hadTimeouts;
  }

  @Override
  public boolean runSucceeded() {
    return runSucceeded;
  }

  private void analyzeLogLine(ContainerResult result, String line) {
    count(line, successLine);
    count(line, errorLine);
    if (result.name.contains("itests-qtest")) {
      findErrorsAndFailures(line, qTestError, qTestFailure);
    } else {
      findErrorsAndFailures(line, unitTestError, unitTestFailure);
    }
    Matcher m = timeout.matcher(line);
    if (m.matches()) hadTimeouts = true;
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

  private void findErrorsAndFailures(String line, Pattern error, Pattern failure) {
    Matcher errorLine = error.matcher(line);
    if (errorLine.matches()) {
      errors.add(errorLine.group(2) + "." + errorLine.group(1));
    } else {
      Matcher failureLine = failure.matcher(line);
      if (failureLine.matches()) {
        failed.add(failureLine.group(2) + "." + failureLine.group(1));
      }
    }
  }
}
