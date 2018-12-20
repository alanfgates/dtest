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
 * DTestLogger is a facade for the logging system.  This allows the use of slf4j, maven plugin logger, or others
 * as appropriate.
 */
public interface DTestLogger {

  void error(String msg);

  void error(String msg, Throwable t);

  void warn(String msg);

  void warn(String msg, Throwable t);

  default void info(String containerId, String msg) {
    info("containerId: " + containerId + " " + msg);
  }

  void info(String msg);

  void info(String msg, Throwable t);

  default void debug(String containerId, String msg) {
    debug("containerId: " + containerId + " " + msg);
  }

  void debug(String msg);

  void debug(String msg, Throwable t);

  boolean isErrorEnabled();

  boolean isWarnEnabled();

  boolean isInfoEnabled();

  boolean isDebugEnabled();

}
