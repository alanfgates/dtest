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
package org.dtest.core.mvn;

import org.dtest.core.BuildState;
import org.dtest.core.BuildYaml;
import org.dtest.core.ContainerResult;
import org.dtest.core.ResultAnalyzer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of ResultAnalyzer with maven specific logic.  This class understands how to parse maven output
 * to interpret test failures, etc.
 */
public class MavenResultAnalyzer extends ResultAnalyzer {

  /**
   * A list of patterns to use to look for errors.  The first pattern that matches will be used, so order matter here.
   */
  protected final Deque<Pattern> unitTestErrorPatterns;

  /**
   * A list of patterns to use to look for failures.  The first pattern that matches will be used, so order matters here.
   */
  protected final Deque<Pattern> unitTestFailurePatterns;

  private AtomicInteger succeeded;
  private List<String> failed;
  private List<String> errors;
  private final Pattern successLine;
  private final Pattern errorLine;
  private final Pattern timeout;

  public MavenResultAnalyzer() {
    // Access to these needs to be synchronized.
    succeeded = new AtomicInteger(0);
    failed = new Vector<>();
    errors = new Vector<>();
    successLine =
        Pattern.compile("\\[(?:INFO|WARNING)\\] Tests run: ([0-9]+), Failures: ([0-9]+), Errors: ([0-9]+).*Time elapsed:.*");
    errorLine =
        Pattern.compile("\\[ERROR\\] Tests run: ([0-9]+), Failures: ([0-9]+), Errors: ([0-9]+).*Time elapsed:.*");
    timeout = Pattern.compile("\\[ERROR\\] Failed to execute goal .* There was a timeout or other error in the fork.*");
    unitTestErrorPatterns = new ArrayDeque<>();
    unitTestErrorPatterns.add(Pattern.compile("\\[ERROR\\] ([A-Za-z0-9_]+).*\\.(Test[A-Za-z0-9_]+).*ERROR!"));
    unitTestFailurePatterns = new ArrayDeque<>();
    unitTestFailurePatterns.add(Pattern.compile("\\[ERROR\\] ([A-Za-z0-9_]+).*\\.(Test[A-Za-z0-9_]+).*FAILURE!"));
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
  public void analyzeLog(ContainerResult result, BuildYaml yaml) throws IOException {
    String[] lines = result.getStdout().split("\n");
    for (String line : lines) {
      count(line, successLine);
      count(line, errorLine);
      analyzeLogLine(result, line, yaml);
    }
    if (buildState.getState() == BuildState.State.HAD_TIMEOUTS) {
      result.setAnalysisResult(ContainerResult.ContainerStatus.TIMED_OUT);
    } else if (buildState.getState() == BuildState.State.HAD_FAILURES_OR_ERRORS) {
      result.setAnalysisResult(ContainerResult.ContainerStatus.FAILED);
    } else {
      // This can get overwritten by later analysis.  It won't overwrite early analysis if there was a failure
      buildState.success();
      result.setAnalysisResult(ContainerResult.ContainerStatus.SUCCEEDED);
    }
  }

  private void analyzeLogLine(ContainerResult result, String line, BuildYaml yaml) throws IOException {
    // Look for timeouts
    Matcher m = timeout.matcher(line);
    if (m.matches()) buildState.sawTimeouts();
    else findErrorsAndFailures(result, line, yaml);
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

  private void findErrorsAndFailures(ContainerResult result, String line, BuildYaml yaml) throws IOException {
    for (Pattern error : unitTestErrorPatterns) {
      Matcher errorLine = error.matcher(line);
      if (errorLine.matches()) {
        String testName = errorLine.group(2) + "." + errorLine.group(1);
        errors.add(testName);
        findLogFiles(result, line, errorLine.group(2), yaml);
        buildState.sawTestFailureOrError();
        break; // If we found an error, don't keep looking or we may double count
      }
    }
    for (Pattern failure : unitTestFailurePatterns) {
      Matcher failureLine = failure.matcher(line);
      if (failureLine.matches()) {
        String testName = failureLine.group(2) + "." + failureLine.group(1);
        failed.add(testName);
        findLogFiles(result, line, failureLine.group(2), yaml);
        buildState.sawTestFailureOrError();
        break; // If we found a failure, don't keep looking or we may double count
      }
    }
  }

  private void findLogFiles(ContainerResult result, String line, String testName, BuildYaml yaml) throws IOException {
    log.debug("Adding log files for container " + result.getCmd().containerSuffix());
    // Make sure we found at least some log files
    boolean foundOne = false;
    for (String pkg : yaml.getJavaPackages()) {
      Pattern p = Pattern.compile(".*(" + pkg + "[a-zA-Z0-9.\\-]*" + testName + ").*");
      Matcher m = p.matcher(line);
      if (m.matches()) {
        foundOne = true;
        // Don't use File.separator here as we are running these in the container, which is guaranteed to be Linux based.
        result.addLogFileToFetch(result.getCmd().containerDirectory() + "/target/surefire-reports/" + m.group(1) + ".txt");
        result.addLogFileToFetch(result.getCmd().containerDirectory() + "/target/surefire-reports/" + m.group(1) + "-output.txt");
      }
    }
    if (!foundOne) throw new IOException("Unable to find logfile for test " + testName + " from line <" + line + ">");
    for (String log : yaml.getAdditionalLogs()) {
      result.addLogFileToFetch(result.getCmd().containerDirectory() + File.separator + log);
    }
  }
}