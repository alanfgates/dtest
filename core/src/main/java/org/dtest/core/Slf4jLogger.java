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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jLogger implements DTestLogger {
  private final Logger log;

  public Slf4jLogger() {
    log = LoggerFactory.getLogger("DTestLogger");
  }

  @Override
  public void error(String msg) {
    log.error(msg);
  }

  @Override
  public void error(String msg, Throwable t) {
    log.error(msg, t);
  }

  @Override
  public void warn(String msg) {
    log.warn(msg);
  }

  @Override
  public void warn(String msg, Throwable t) {
    log.warn(msg, t);
  }

  @Override
  public void info(String msg) {
    log.info(msg);
  }

  @Override
  public void info(String msg, Throwable t) {
    log.info(msg, t);
  }

  @Override
  public void debug(String msg) {
    log.debug(msg);
  }

  @Override
  public void debug(String msg, Throwable t) {
    log.debug(msg, t);
  }

  @Override
  public boolean isErrorEnabled() {
    return log.isErrorEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return log.isWarnEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return log.isInfoEnabled();
  }

  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }
}
