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
 * A simple super class that keeps track of the config object and the log.
 */
public abstract class Configurable {

  /**
   * Configuration object for this build.
   */
  protected Config cfg;

  /**
   * Logger.  This should be used by all classes for logging as it coordinates which underlying logging system
   * is currently being used.
   */
  protected DTestLogger log;

  /**
   * Set the configuration object for this object.
   * @param cfg config
   * @return reference to this object.
   */
  public Configurable setConfig(Config cfg) {
    this.cfg = cfg;
    return this;
  }

  /**
   * Set the log object for this object.
   * @param log log
   * @return reference to this object.
   */
  public Configurable setLog(DTestLogger log) {
    this.log = log;
    return this;
  }

}
