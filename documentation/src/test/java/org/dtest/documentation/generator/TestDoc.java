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

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class TestDoc {

  @Test
  public void goodDoc() throws IOException {
    Doc doc = new Doc("fred");
    Section section = new Section("first");
    section.setAfter(Section.BEGIN);
    section.setText("Mary had a little lamb\n");
    doc.addSection(section);
    section = new Section("second");
    section.setAfter("first");
    section.setText("Its fleece was white as snow\n");
    doc.addSection(section);
    section = new Section("fourth");
    section.setAfter("third");
    section.setText("The lamb was sure to go\n");
    doc.addSection(section);
    section = new Section("third");
    section.setAfter("second");
    section.setText("And everywhere there Mary went\n");
    doc.addSection(section);

    Assert.assertEquals("Mary had a little lamb\n" +
        "Its fleece was white as snow\n" +
        "And everywhere there Mary went\n" +
        "The lamb was sure to go\n", doc.buildDoc());

  }

  @Test
  public void noBeginning() throws IOException {
    Doc doc = new Doc("fred");
    Section section = new Section("first");
    section.setAfter("second");
    section.setText("abc\n");
    doc.addSection(section);
    section = new Section("second");
    section.setAfter("first");
    section.setText("def\n");
    doc.addSection(section);

    try {
      doc.buildDoc();
      Assert.fail("should have failed to build the doc");
    } catch (IOException e) {
      Assert.assertEquals("No beginning found for document fred", e.getMessage());
    }
  }

  @Test
  public void multipleAfterSame() throws IOException {
    Doc doc = new Doc("fred");
    Section section = new Section("first");
    section.setAfter(Section.BEGIN);
    section.setText("abc\n");
    doc.addSection(section);
    section = new Section("second");
    section.setAfter("first");
    section.setText("def\n");
    doc.addSection(section);
    section = new Section("third");
    section.setAfter("first");
    section.setText("ghi\n");
    try {
      doc.addSection(section);
      Assert.fail("should have failed to build the doc");
    } catch (IOException e) {
      Assert.assertEquals("Found two sections both listed as after section first. third and second", e.getMessage());
    }
  }

  @Test
  public void gapInSections() throws IOException {
    Doc doc = new Doc("fred");
    Section section = new Section("first");
    section.setAfter(Section.BEGIN);
    section.setText("abc\n");
    doc.addSection(section);
    section = new Section("third");
    section.setAfter("second");
    section.setText("def\n");
    doc.addSection(section);

    try {
      doc.buildDoc();
      Assert.fail("should have failed to build the doc");
    } catch (IOException e) {
      Assert.assertEquals("Found a gap in the document.  No section was found immediately after first but sections" +
          " third have not been placed in the document.", e.getMessage());
    }
  }
}
