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

/**
 * This captures the state of the entire build.  It handles the appropriate transitions given that we may
 * see results from different containers in different orders.  It's important with this class to understand
 * the distinction between the build had failures and the build itself failed and the build had timeouts and
 * the build itself timed out.
 */
public class BuildState {

  // The order matters here, as it goes intentionally from best to worst.  The state transitions depend on these
  // being in the right order.
  public enum State {

    /**
     * Initial state.
     */
    NOT_INITIALIZED("We don't know, this shouldn't happen"),

    /**
     * The build ran to completion and all tests passed.
     */
    SUCCEEDED("the build ran to completion and all tests passed"),

    /**
     * The build ran to completion but some tests failed or had errors.
     */
    HAD_FAILURES_OR_ERRORS("the build ran to completion but some tests failed or had errors"),

    /**
     * The build ran to completion but some tests timed out.  The results are incomplete.
     */
    HAD_TIMEOUTS("the build ran to completion but some containers timed out"),

    /**
     * The build failed to complete.  The results are incomplete.
     */
    FAILED("the build did not run to completion"),

    /**
     * The build timed out.  The results are incomplete.
     */
    TIMED_OUT("the build timed out");

    private String explanation;

    State(String explanation) {
      this.explanation = explanation;
    }

    /**
     * Get a summary of the build state along with a brief explanation of its meaning.
     * @return summary
     */
    public String getSummary() {
      return this.name().replace('_', ' ').toUpperCase() + ", " + explanation + ".";
    }
  }

  private State state = State.NOT_INITIALIZED;

  public State getState() {
    return state;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BuildState) {
      return state == ((BuildState)obj).state;
    }
    return false;
  }

  @Override
  public String toString() {
    return state.getSummary();
  }

  /**
   * Update this state based on the state returned from a task.
   * @param other state from a task.
   */
  public void update(BuildState other) {
    setStateIfNotAlreadyInHigherState(other.state);
  }

  /**
   * Set the state to {@link State#SUCCEEDED}.  If the build has already seen failures or timeouts or has itself failed or timedout
   * this will be ignored.
   */
  public void success() {
    setStateIfNotAlreadyInHigherState(State.SUCCEEDED);
  }

  /**
   * Set the state to {@link State#HAD_FAILURES_OR_ERRORS}.  This will override a successful state.  If the build
   * has already seen timeouts or has itself failed or timed out this will be ignored.
   */
  public void sawTestFailureOrError() {
    setStateIfNotAlreadyInHigherState(State.HAD_FAILURES_OR_ERRORS);
  }

  /**
   * Set the state to {@link State#HAD_TIMEOUTS}.  This will override success or had failures.  If the build has
   * already failed or timedout this will be ignored.
   */
  public void sawTimeouts() {
    setStateIfNotAlreadyInHigherState(State.HAD_TIMEOUTS);
  }

  /**
   * Set the state to {@link State#FAILED}.  This will override success, had failures, or had timeouts.  If the build
   * has already timed out it will be ignored.
   */
  public void fail() {
    setStateIfNotAlreadyInHigherState(State.FAILED);
  }

  /**
   * Set the state to {@link State#TIMED_OUT}.  This overrides all other states.
   */
  public void timeout() {
    setStateIfNotAlreadyInHigherState(State.TIMED_OUT);
  }

  // synchronize this as multiple tasks can be reporting in at the same time.
  private synchronized void setStateIfNotAlreadyInHigherState(State newState) {
    // only set this if haven't seen a bigger issue
    if (state.ordinal() < newState.ordinal()) state = newState;
  }
}
