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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

public class TestDocParser {

  @Test
  public void goodDoc() throws IOException {
    File file = new File(System.getProperty("conf.dir") + File.separator + "test-classes", "GoodDoc.java");
    DocParser parser = new DocParser();
    parser.parseFile(file);
    Collection<Doc> docs = parser.getDocs();
    Assert.assertEquals(2, docs.size());
    Doc good = null, evenbetter = null;
    for (Doc doc: docs) {
      if (doc.getName().equals("good")) good = doc;
      else if (doc.getName().equals("evenbetter")) evenbetter = doc;
      else Assert.fail("Unexpected doc " + doc.getName());
    }
    Assert.assertEquals(
        "# Introduction\n" +
        "This is the introduction.\n" +
        "It is very well written.\n" +
        "\n" +
        "This is the body.  It is even better written than the introdution.\n" +
        "\n" +
        "*Something bold*\n" +
        "\n" +
        "A list\n" +
        "- one\n" +
        "- two\n" +
        "- three\n" +
        "\n" +
        "A [link](www.hortonworks.com)\n" +
        "\n" +
        "It is super interesting.\n" +
        "\n" +
        "## Conclusion\n" +
        "This is the conclusion.  I purposefully inverted the tag order here to see what happens.\n", good.buildDoc());
    Assert.assertEquals("This is a second document, inserted to make sure the parser can handle multiple documents.\n\n" +
            "This is a comment with no after, to make sure things are ordered properly.\n",
        evenbetter.buildDoc());
  }

  @Test
  public void noDocuments() throws IOException {
    File file = new File(System.getProperty("conf.dir") + File.separator + "test-classes", "NoComments.java");
    DocParser parser = new DocParser();
    parser.parseFile(file);
    Collection<Doc> docs = parser.getDocs();
    Assert.assertEquals(0, docs.size());
  }

  @Test(expected = FileNotFoundException.class)
  public void noSuchFile() throws IOException {
    File file = new File("nosuch");
    DocParser parser = new DocParser();
    parser.parseFile(file);
  }

  @Test
  public void twoDocTags() throws IOException {
    File f = writeFile(new String[] {
        "/*~~",
        " * @document baddoc",
        " * @document baddoc",
        " * bad, can't have 2 doc tags in one section."
    });
    DocParser parser = new DocParser();
    try {
      parser.parseFile(f);
      Assert.fail("should have failed to parse doc");
    } catch (IOException e) {
      Assert.assertEquals("Each section must have only one @document tag.", e.getMessage());
    }
  }

  @Test
  public void twoSectionTags() throws IOException {
    File f = writeFile(new String[] {
        "/*~~",
        " * @document baddoc",
        " * @section one",
        " * @section two",
        " * bad, can't have 2 section tags in one section."
    });
    DocParser parser = new DocParser();
    try {
      parser.parseFile(f);
      Assert.fail("should have failed to parse doc");
    } catch (IOException e) {
      Assert.assertEquals("Each section must have only one @section tag.", e.getMessage());
    }
  }

  @Test
  public void twoAfterTags() throws IOException {
    File f = writeFile(new String[] {
        "/*~~",
        " * @document baddoc",
        " * @section one",
        " * @after one",
        " * @after two",
        " * bad, can't have 2 after tags in one section."
    });
    DocParser parser = new DocParser();
    try {
      parser.parseFile(f);
      Assert.fail("should have failed to parse doc");
    } catch (IOException e) {
      Assert.assertEquals("Multiple @after or both @after and @begin tags not allowed in a single section", e.getMessage());
    }
  }

  @Test
  public void beginAndAfterTags() throws IOException {
    File f = writeFile(new String[] {
        "/*~~",
        " * @document baddoc",
        " * @section one",
        " * @begin",
        " * @after one",
        " * bad, can't have after and begin tags in one section."
    });
    DocParser parser = new DocParser();
    try {
      parser.parseFile(f);
      Assert.fail("should have failed to parse doc");
    } catch (IOException e) {
      Assert.assertEquals("Multiple @after or both @after and @begin tags not allowed in a single section", e.getMessage());
    }
  }

  @Test
  public void afterAndBeginTags() throws IOException {
    File f = writeFile(new String[] {
        "/*~~",
        " * @document baddoc",
        " * @section one",
        " * @after one",
        " * @begin",
        " * bad, can't have after and begin tags in one section."
    });
    DocParser parser = new DocParser();
    try {
      parser.parseFile(f);
      Assert.fail("should have failed to parse doc");
    } catch (IOException e) {
      Assert.assertEquals("Both @begin and @after tags or multiple @begin tags not allowed in a single section", e.getMessage());
    }
  }

  @Test
  public void twoBeginTags() throws IOException {
    File f = writeFile(new String[] {
        "/*~~",
        " * @document baddoc",
        " * @section one",
        " * @begin",
        " * @begin",
        " * bad, can't have after and begin tags in one section."
    });
    DocParser parser = new DocParser();
    try {
      parser.parseFile(f);
      Assert.fail("should have failed to parse doc");
    } catch (IOException e) {
      Assert.assertEquals("Both @begin and @after tags or multiple @begin tags not allowed in a single section", e.getMessage());
    }
  }

  private File writeFile(String[] lines) throws IOException {
    File tmp = File.createTempFile("testDocParser", ".java");
    FileWriter writer = new FileWriter(tmp);
    for (String line : lines) writer.write(line + "\n");
    writer.close();
    return tmp;
  }

}
