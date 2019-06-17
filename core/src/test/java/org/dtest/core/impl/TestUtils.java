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
package org.dtest.core.impl;

import org.junit.Assert;
import org.junit.Test;

public class TestUtils {

  @Test
  public void goodLabel() {
    String label = Utils.generateLabel("hive-12995");
    Assert.assertEquals("hive-12995", label);
  }

  @Test
  public void longLabel() {
    String label = Utils.generateLabel("When in the Course of human events, it becomes necessary for one people" +
        "to dissolve the political bands which have connected them with another, and to assume among the powers of" +
        "the earth, the separate and equal station to which the Laws of Nature and of Nature's God entitle them, a" +
        "decent respect to the opinions of mankind requires that they should declare the causes which impel them to" +
        "the separation.");
    Assert.assertEquals("WhenXinXtheXCourseXofXhumanXeventsXXitXbecomesXnecessaryXforXoneXpeople" +
        "toXdissolveXtheXpoliticalXbandsXwhichXhaveXconnectedXthem", label);
  }
}
