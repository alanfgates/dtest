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

import java.util.List;

public interface ResultAnalyzer {

  /**
   * Analyze a log
   * @param containerResult the result from the container run.  It is ok for this method to make
   *                        changes in the result.
   */
  void analyzeLog(ContainerResult containerResult);

  /**
   * Get count of succeeded tests.
   * @return number of tests that succeeded.
   */
  int getSucceeded();

  /**
   * Get list of tests that failed.
   * @return name of each test that failed.
   */
  List<String> getFailed();

  /**
   * Get list of tests that ended in error.
   * @return name of each test that produced an error.
   */
  List<String> getErrors();

  /**
   * True if at least one test timed out.
   * @return true if any tests timed out.
   */
  boolean hadTimeouts();

  /**
   * True if the test run succeeded.  Note that this does not mean all tests passed, but that all
   * tests were run and the container exited normally.  Some tests may have failed or timed out.
   * @return true if success.
   */
  boolean runSucceeded();

}
