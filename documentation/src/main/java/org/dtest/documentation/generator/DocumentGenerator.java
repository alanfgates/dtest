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

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A tool to read the source code and find any document comments and
 * use these to build documentation for the project.  This is intended to be used in the site phase of the mvn site
 * lifecycle.  It depends on being able to find the source code.
 */
public class DocumentGenerator {

  private final File srcDir;               // source directory to look in
  private final File targetDir;            // target directory to write into
  private List<File> srcFiles;             // all the srcFiles we've found

  private DocumentGenerator(String srcDir, String targetDir) throws IOException {
    this.srcDir = new File(srcDir);
    if (!this.srcDir.exists() || !this.srcDir.isDirectory()) {
      throw new IOException(srcDir + " no such directory");
    }
    this.targetDir = new File(targetDir);
    if (!this.targetDir.exists() || !this.targetDir.isDirectory()) {
      throw new IOException(targetDir + " no such directory");
    }
    srcFiles = new ArrayList<>();
  }

  private void generate() throws IOException {

    // First, find all the source files
    findSrcFiles(srcDir);

    // Parse each source file
    DocParser parser = new DocParser();
    for (File srcFile : srcFiles) {
      parser.parseFile(srcFile);
    }

    // Write out the docs
    writeOutDocs(parser.getDocs());
  }

  private void findSrcFiles(File currentDir) {
    assert currentDir.isDirectory();
    File[] files = currentDir.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        findSrcFiles(file);
      } else if (file.isFile() && file.getName().endsWith(".java")){
        srcFiles.add(file);
      }
    }
  }

  private void writeOutDocs(Collection<Doc> docs) throws IOException {
    for (Doc doc : docs) {
      // Open the file to write it to
      File fd = new File(targetDir, doc.getName() + ".html");
      FileWriter writer = new FileWriter(fd);

      // Render the document into HTML
      Parser parser = Parser.builder().build();
      Node document = parser.parse(doc.buildDoc());
      HtmlRenderer renderer = HtmlRenderer.builder().build();
      writer.write(renderer.render(document));
      writer.close();
    }
  }

  /**
   *
   * @param args expects a two arguments, first the top level directory where source is found; usually this will be
   *             <i>project</i><code>/src/main/java</code>, second the target directory; usually this will be
   *             <i>project</i><code>/target/site</code>.
   * @throws IOException if a file or directory cannot be found or created
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      throw new IOException("Expects two arguments, the top level source directory, usually _project_/src/main/java" +
          "and the target site directory, usually _project_/target/site");
    }

    DocumentGenerator generator = new DocumentGenerator(args[0], args[1]);
    generator.generate();
  }

}
