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

import java.io.File;
import java.io.IOException;

/**
 * Reporters concisely organize the results of the running DTest for users.
 */
public abstract class Reporter extends Configurable {

  public static final String CFG_REPORTER_IMPL = "dtest.core.reporter.impl";
  private static final Class<? extends Reporter> CFG_REPORTER_IMPL_DEFAULT = HtmlReporter.class;

  /**
   * Repository used for this build.
   */
  protected String repo;

  /**
   * Branch in the repo that was built.
   */
  protected String branch;

  /**
   * Profile used for this build.
   */
  protected String profile;

  /**
   * Information on this build.
   */
  protected BuildInfo buildInfo;

  protected int numErrors;
  protected int numFailures;
  protected int numSucceeded;
  protected String status;


  public Reporter setRepo(String repo) {
    this.repo = repo;
    return this;
  }

  public Reporter setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public Reporter setProfile(String profile) {
    this.profile = profile;
    return this;
  }

  public Reporter setBuildInfo(BuildInfo buildInfo) {
    this.buildInfo = buildInfo;
    return this;
  }

  /**
   * Get directory where logs from a given container should be moved for the report.  The directory should exist
   * after calling this method.
   * @param result result from running the container whose logs we are getting a directory for
   * @return directory
   */
  public abstract File getLogDirForContainer(ContainerResult result) throws IOException;

  /**
   * Add a section to the report for failed tests.  Can be called multiple times.  Implementations should be
   * re-entrant, as many threads may be calling them simultaneously.
   * @param docker docker client, needed to fetch the log files
   * @param result result from running one container
   * @throws IOException if we cannot copy the logs or write out the report section
   */
  public abstract void addFailedTests(ContainerClient docker, ContainerResult result)
      throws IOException;

  /**
   * After all the tests are done, this will take the summary information and add it to the report.  This should
   * only be called once.
   * @param analyzer analyzed results of running the tests
   */
  public void summarize(ResultAnalyzer analyzer) {
    numErrors = analyzer.getErrors().size();
    numFailures = analyzer.getFailed().size();
    numSucceeded = analyzer.getSucceeded();
    status = analyzer.getBuildState().getState().name().replace('_', ' ');
  }

  /**
   * Publishes the report.  This should not be called until all the other methods in the interface have been called.
   * @throws IOException if publishing fails
   */
  public abstract void publish() throws IOException;

  static Reporter getInstance(Config cfg, DTestLogger log) throws IOException {
    Reporter r = Utils.getInstance(cfg.getAsClass(CFG_REPORTER_IMPL, Reporter.class, CFG_REPORTER_IMPL_DEFAULT));
    r.setConfig(cfg).setLog(log);
    log.debug("Constructed Reporter of type " + r.getClass().getName());
    return r;
  }

}
