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
package org.dtest.documentation.generator;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TestDocumentGenerator {

  @Test
  public void generate() throws IOException {
    String target = System.getProperty("conf.dir");
    String resources = target + File.separator + "test-classes";
    DocumentGenerator.main(new String[] {resources, target});
    Assert.assertThat(new File(target, "good.html"), new FileMatcher(new File(resources, "expected_good.html")));
    Assert.assertThat(new File(target, "evenbetter.html"), new FileMatcher(new File(resources, "expected_evenbetter.html")));
  }

  public static class FileMatcher extends BaseMatcher<File> {
    private String reason;
    private final File golden;

    public FileMatcher(File golden) {
      this.golden = golden;
    }

    @Override
    public boolean matches(Object o) {
      if (!(o instanceof File)) {
        reason = "Passed in object is not a File";
        return false;
      }

      File testFile = (File)o;
      BufferedReader goldenReader = null, testReader = null;
      try {
        goldenReader = new BufferedReader(new FileReader(golden));
        testReader = new BufferedReader(new FileReader(testFile));
        int lineNum = 0;
        while (true) {
          String goldenLine = goldenReader.readLine();
          String testLine = testReader.readLine();
          lineNum++;
          if (goldenLine == null && testLine == null) return true;
          if (goldenLine == null || testLine == null) {
            if (goldenLine == null) reason = "Golden file ended before test file";
            else reason = "Test file ended before golden file";
            return false;
          }
          if (!goldenLine.equals(testLine)) {
            reason = "Files differ at line " + lineNum;
            return false;
          }
        }
      } catch (IOException e) {
        reason = "Caught IOException: " + e.getMessage();
        return false;
      } finally {
        try {
          goldenReader.close();
          testReader.close();
        } catch (IOException e) {
          // don't know what to do about it, exceptions in close are stupid
        }
      }
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(reason);
    }
  }
}
