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
import org.dtest.core.ContainerResult;
import org.dtest.core.ResultAnalyzer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
   * When a test times out it is not easy to figure out from the logs which test timed out.  Maven just reports no results for
   * that test and at the end says, "btw, there were timeouts".  So we don't find the exact test name, we just record that
   * some test timed out.
   */
  public static final String TIMED_OUT_KEY = "Timed out";

  private AtomicInteger succeeded;
  private List<String> failed;
  private List<String> errors;
  private final Pattern timeout;
  private BuildState lastContainerState;

  public MavenResultAnalyzer() {
    // Access to these needs to be synchronized.
    succeeded = new AtomicInteger(0);
    failed = new Vector<>();
    errors = new Vector<>();
    timeout = Pattern.compile(".*Failed to execute goal .* There was a timeout or other error in the fork.*");
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
  public void analyzeLog(ContainerResult result) throws IOException {
    lastContainerState = new BuildState();
    String[] lines = result.getStdout().split("\n");
    for (String line : lines) {
      assert line != null;
      lookForTimeouts(result, line);
    }
    examineReports(result);
    try {
      if (lastContainerState.getState() == BuildState.State.HAD_TIMEOUTS) {
        result.setAnalysisResult(ContainerResult.ContainerStatus.TIMED_OUT);
      } else if (lastContainerState.getState() == BuildState.State.HAD_FAILURES_OR_ERRORS) {
        result.setAnalysisResult(ContainerResult.ContainerStatus.FAILED);
      } else {
        // This can get overwritten by later analysis.  It won't overwrite early analysis if there was a failure
        lastContainerState.success();
        result.setAnalysisResult(ContainerResult.ContainerStatus.SUCCEEDED);
      }
    } finally {
      buildState.update(lastContainerState);
    }
  }

  @Override
  public String getTestResultsDir() {
    return "target" + File.separator + "surefire-reports";
  }

  protected String determineTestCaseName(String caseName) {
    return caseName;
  }

  protected String testNameForLogs(String testName, String caseName) {
    return testName;
  }

  private void lookForTimeouts(ContainerResult result, String line) throws IOException {
    // Look for timeouts
    Matcher m = timeout.matcher(line);
    if (m.matches()) {
      lastContainerState.sawTimeouts();
      result.getReports().keepAdditionalLogs(MavenResultAnalyzer.TIMED_OUT_KEY);
    }
  }

  private void examineReports(ContainerResult result) throws IOException {
    // find all the xml files
    File[] xmlFiles = result.getReports().getDir().listFiles((dir, name) -> name.endsWith(".xml"));
    if (xmlFiles == null) {
      log.warn("Unable to find any xml files for container " + result.getContainerName() + " not sure if this is ok or not.");
      return;
    }

    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      for (File xmlFile : xmlFiles) {
        SAXParser parser = factory.newSAXParser();
        Handler handler = new Handler();
        parser.parse(xmlFile, handler);
        succeeded.addAndGet(handler.report.numRun - handler.report.errors - handler.report.failures - handler.report.skipped);
        for (TestCase tc : handler.report.cases) {
          if (tc.result != TestResult.SUCCESS) {
            String testName = handler.report.name.substring(handler.report.name.lastIndexOf('.') + 1);
            lastContainerState.sawTestFailureOrError();
            if (tc.result == TestResult.FAILURE) failed.add(testName + "." + determineTestCaseName(tc.name));
            else if (tc.result == TestResult.ERROR) errors.add(testName + "." + determineTestCaseName(tc.name));
            else throw new RuntimeException("Unexpected enum value");
            File[] toFetch = result.getReports().getDir().listFiles(
                (dir, name) -> name.contains(handler.report.name + ".txt") || name.contains(handler.report.name + "-output.txt"));
            if (toFetch == null) log.warn("Unable to find any logfile for testcase " + testNameForLogs(testName, tc.name));
            else for (File fetchie : toFetch) result.getReports().keep(fetchie, testNameForLogs(testName, tc.name));

          }
        }
      }
    } catch (SAXException| ParserConfigurationException e) {
      throw new IOException(e);
    }
  }

  private static class Report {
    String name;
    List<TestCase> cases = new ArrayList<>();
    int numRun, errors, failures, skipped;
  }

  private enum TestResult { SUCCESS, ERROR, FAILURE }

  private static class TestCase {
    TestResult result;
    String name;
  }

  private static class Handler extends DefaultHandler {
    Report report = new Report();
    TestCase currentTestCase = null;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
      if ("testsuite".equals(qName)) {
        report.name = attributes.getValue("name");
        report.numRun = Integer.parseInt(attributes.getValue("tests"));
        report.errors = Integer.parseInt(attributes.getValue("errors"));
        report.failures = Integer.parseInt(attributes.getValue("failures"));
        report.skipped = Integer.parseInt(attributes.getValue("skipped"));
      } else if ("testcase".equals(qName)) {
        TestCase tc = new TestCase();
        tc.name = attributes.getValue("name");
        report.cases.add(tc);
        currentTestCase = tc;
        currentTestCase.result = TestResult.SUCCESS;
      } else if ("failure".equals(qName)) {
        assert currentTestCase != null;
        currentTestCase.result = TestResult.FAILURE;
      } else if ("error".equals(qName)) {
        assert currentTestCase != null;
        currentTestCase.result = TestResult.ERROR;
      }
    }
  }
}
