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

import org.dtest.core.git.GitSource;
import org.junit.Test;

import java.io.IOException;

public class TestBuildInfo {

  @Test
  public void simple() throws IOException {
    new BuildInfo(new GitSource(), "patch1", "profile1");
  }

  @Test
  public void withDash() throws IOException {
    new BuildInfo(new GitSource(), "patch1-run2", "profile1");
  }

  @Test(expected = IOException.class)
  public void withSlash() throws IOException {
    new BuildInfo(new GitSource(), "patch1/run2", "profile1");
  }
}
