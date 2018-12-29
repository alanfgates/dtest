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
    Assert.assertEquals("This is a second document, inserted to make sure the parser can handle multiple documents.\n\n",
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

}
