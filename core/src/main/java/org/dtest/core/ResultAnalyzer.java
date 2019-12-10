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

  /**
   * State of this build based on analyzing logs from the test containers.
   */
  protected final BuildState buildState;

  protected ResultAnalyzer() {
    buildState = new BuildState();
  }

  /**
   * Class to analyze results of the tests.  Defaults to MavenResultAnalyzer.
   */
  public final static String CFG_RESULTANALYZER_IMPL = "dtest.core.resultanalyzer.impl";

  /**
   * Analyze a the result of running a container.  Implementations of this method must
   * be thread safe.
   * @param containerResult the result from the container run.  Information in the result will be
   *                       appended by this method.
   * @param cmd command for this container, passed because it contains information on failures to ignore.
   * @throws IOException if it fails to find the information it needs when analyzing the log.
   */
  public abstract void analyzeResult(ContainerResult containerResult, ContainerCommand cmd) throws IOException;

  /**
   * Get the directory where we expect to find test result files.
   * @return directory name.
   */
  public abstract String getTestResultsDir();

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
   * Get the global state of the build based on the results analysis.
   * @return state of the build.  If accessed before the build is finished, this will be an incomplete state as it
   * can't tell if the build as a whole failed or timed out.
   */
  public BuildState getBuildState() {
    return buildState;
  }

  static ResultAnalyzer getInstance(Config cfg, DTestLogger log) throws IOException {
    ResultAnalyzer ra = Utils.getInstance(cfg.getAsClass(ResultAnalyzer.CFG_RESULTANALYZER_IMPL,
        ResultAnalyzer.class, MavenResultAnalyzer.class));
    ra.setConfig(cfg).setLog(log);
    log.debug("Instantiated ReturnAnalyze of type " + ra.getClass().getName());
    return ra;
  }
}
