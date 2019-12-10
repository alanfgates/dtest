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
package org.dtest.hive;

import org.dtest.core.mvn.MavenResultAnalyzer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hive specialization of MavenResultAnalyzer.  Handles determining test names for qfile tests.
 */
public class HiveResultAnalyzer extends MavenResultAnalyzer {

  private static final Pattern qFileNameFinder = Pattern.compile("testCliDriver\\[([A-Za-z0-9_]+)\\]");

  @Override
  protected String determineTestCaseName(String caseName) {
    Matcher m = qFileNameFinder.matcher(caseName);
    if (m.matches()) {
      return m.group(1);
    } else {
      return caseName;
    }
  }

  @Override
  protected String testNameForLogs(String testName, String caseName) {
    Matcher m = qFileNameFinder.matcher(caseName);
    if (m.matches()) {
      return testName + "." + m.group(1);
    } else {
      return testName;
    }
  }
}
