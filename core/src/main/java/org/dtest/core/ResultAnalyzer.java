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
package org.dtest.core;

import org.dtest.core.impl.Utils;
import org.dtest.core.mvn.MavenResultAnalyzer;

import java.io.IOException;
import java.util.List;

/**
 * ResultAnalyzer analyzes the output of tests.  The implementation is tied to {@link ContainerCommand} since it
 * must understand the results of the build commands.
 */
public abstract class ResultAnalyzer extends Configurable {
  // Implementation of ResultAnalyzer
  /**
   * Class to analyze results of the tests.  Defaults to MavenResultAnalyzer.
   */
  public final static String CFG_RESULTANALYZER_IMPL = "dtest.core.resultanalyzer.impl";

  /**
   * Analyze a log.  This can be called a number of times on logs returned by containers.
   * @param containerResult the result from the container run.  Information in the result will be
   *                       appended by this method.
   */
  public abstract void analyzeLog(ContainerResult containerResult);

  /**
   * Get aggregate count of succeeded tests.
   * @return number of tests that succeeded.
   */
  public abstract int getSucceeded();

  /**
   * Get list of tests that failed.
   * @return name of each test that failed.
   */
  public abstract List<String> getFailed();

  /**
   * Get list of tests that ended in error.
   * @return name of each test that produced an error.
   */
  public abstract List<String> getErrors();

  /**
   * True if at least one test timed out.
   * @return true if any tests timed out.
   */
  public abstract boolean hadTimeouts();

  /**
   * True if the test run succeeded.  Note that this does not mean all tests passed, but that all
   * tests were run and the container exited normally.  Some tests may have failed or timed out.
   * @return true if success.
   */
  public abstract boolean runSucceeded();

  static ResultAnalyzer getInstance(Config cfg) throws IOException {
    ResultAnalyzer ra = Utils.getInstance(cfg.getAsClass(ResultAnalyzer.CFG_RESULTANALYZER_IMPL,
        ResultAnalyzer.class, MavenResultAnalyzer.class));
    ra.setConfig(cfg);
    return ra;
  }
}
