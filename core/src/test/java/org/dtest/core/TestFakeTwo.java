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

import org.junit.Assume;
import org.junit.Test;

public class TestFakeTwo {
  static final String SHOULD_RUN = "dtest.run.fake";

  @Test
  public void passTwo() {

  }

  @Test
  public void errorTwo() {
    Assume.assumeNotNull(System.getProperty(SHOULD_RUN));
    int[] array = new int[5];
    array[5] = 3;
  }

}
