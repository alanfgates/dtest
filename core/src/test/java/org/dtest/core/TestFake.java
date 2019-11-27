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

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/**
 * These are not real unit tests.  They are tests used by ITest to create intentional failures and errors.
 */
public class TestFake {
  static final String SHOULD_RUN = "dtest.run.fake";

  @Test
  public void pass() {

  }

  @Test
  public void fail() {
    Assume.assumeNotNull(System.getProperty(SHOULD_RUN));
    Assert.assertEquals(1, 0);
  }

  /*
  @Test
  public void longTime() throws InterruptedException {
    Thread.sleep(1000000000000L);
  }
   */

}
