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
package org.dtest.core.simple;

import org.dtest.core.ContainerResult;
import org.dtest.core.ResultAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleResultAnalyzer implements ResultAnalyzer {
  private static final Logger LOG = LoggerFactory.getLogger(SimpleResultAnalyzer.class);

  private boolean hadTimeouts;
  private boolean runSucceeded;
  private AtomicInteger succeeded;
  private List<String> failed;
  private List<String> errors;
  private final Pattern successLine;
  private final Pattern errorLine;
  private final Pattern unitTestError;
  private final Pattern unitTestFailure;
  private final Pattern timeout;

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
    successLine =
        Pattern.compile("\\[(?:INFO|WARNING)\\] Tests run: ([0-9]+), Failures: ([0-9]+), Errors: ([0-9]+).*Time elapsed:.*");
    errorLine =
        Pattern.compile("\\[ERROR\\] Tests run: ([0-9]+), Failures: ([0-9]+), Errors: ([0-9]+).*Time elapsed:.*");
    unitTestError =
        Pattern.compile("\\[ERROR\\] ([A-Za-z0-9_]+).*\\.(Test[A-Za-z0-9_]+).*ERROR!");
    unitTestFailure =
        Pattern.compile("\\[ERROR\\] ([A-Za-z0-9_]+).*\\.(Test[A-Za-z0-9_]+).*FAILURE!");
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
    String[] lines = result.getLogs().split("\n");
    boolean sawTimeout = false;
    for (String line : lines) {
      count(line, successLine);
      count(line, errorLine);
      sawTimeout |= analyzeLogLine(result, line);
    }
    if (sawTimeout) {
      hadTimeouts = true;
      result.setAnalysisResult(ContainerResult.ContainerStatus.TIMED_OUT);
    } else if (result.getRc() != 0) {
      runSucceeded = false;
      result.setAnalysisResult(ContainerResult.ContainerStatus.FAILED);
    } else {
      result.setAnalysisResult(ContainerResult.ContainerStatus.SUCCEEDED);
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
  protected boolean analyzeLogLine(ContainerResult result, String line) {
    if (!findErrorsAndFailures(result, line, unitTestError, unitTestFailure)) {
      // Finally, look for timeouts
      Matcher m = timeout.matcher(line);
      if (m.matches()) {
        return true;
      }
    }
    return false;
  }

  protected void count(String line, Pattern pattern) {
    Matcher m = pattern.matcher(line);
    if (m.matches()) {
      int total = Integer.parseInt(m.group(1));
      int failures = Integer.parseInt(m.group(2));
      int errors = Integer.parseInt(m.group(3));
      succeeded.addAndGet(total - failures - errors);
    }
  }

  protected boolean findErrorsAndFailures(ContainerResult result, String line, Pattern error,
                                        Pattern failure) {
    Matcher errorLine = error.matcher(line);
    if (errorLine.matches()) {
      String testName = errorLine.group(2) + "." + errorLine.group(1);
      errors.add(testName);
      findLogFiles(result, line, errorLine.group(2));
      return true;
    } else {
      Matcher failureLine = failure.matcher(line);
      if (failureLine.matches()) {
        String testName = failureLine.group(2) + "." + failureLine.group(1);
        failed.add(testName);
        findLogFiles(result, line, failureLine.group(2));
        return true;
      }
    }
    return false;
  }

  private void findLogFiles(ContainerResult result, String line, String testName) {
    Pattern p = Pattern.compile(".*(" + getTestClassPrefix() + testName + ").*");
    Matcher m = p.matcher(line);
    if (!m.matches()) {
      throw new RuntimeException("Failed to find the full name of the failed test.");
    }
    LOG.debug("Adding log files for container " + result.getCmd().containerSuffix());
    result.addLogFileToFetch(result.getCmd().containerDirectory() + "/target/tmp/log/hive.log");
    result.addLogFileToFetch(result.getCmd().containerDirectory() + "/target/surefire-reports/" + m.group(1) + ".txt");
    result.addLogFileToFetch(result.getCmd().containerDirectory() + "/target/surefire-reports/" + m.group(1) + "-output.txt");
  }

  protected String getTestClassPrefix() {
    return "org\\.dtest\\.";
  }
}
