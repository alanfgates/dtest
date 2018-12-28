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
import org.dtest.documentation.annotation.Document;
import org.dtest.documentation.annotation.DocumentEntry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A tool to read the source code and find any {@link org.dtest.documentation.annotation.Document} annotations and
 * use these to build documentation for the project.  This is intended to be used in the site phase of the mvn site
 * lifecycle.  It depends on being able to find the source code.
 */
public class DocumentGenerator {

  private final String srcDir;               // source directory to look in
  private final String targetDir;            // target directory to write into
  private List<String> classes;              // all the classes we've found
  private List<Doc> docs;                    // list of all document annotations we've seen
  private Map<StringPair, List<String>> entries; // all entries annotations we've seen, keyed to document and section

  private DocumentGenerator(String srcDir, String targetDir) {
    this.srcDir = srcDir;
    this.targetDir = targetDir;
    classes = new ArrayList<>();
  }

  private void generate() throws IOException, ClassNotFoundException {

    // First, find all the source files
    findSrcFiles();

    docs = new ArrayList<>();
    entries = new HashMap<>();

    // Load each class and look for annotations
    lookForAnnotations();

    // Match up the docs with the entries
    matchupDocsAndEntries();

    // Write out the docs
    writeOutDocs();
  }

  private void findSrcFiles() throws IOException {
    File src = new File(srcDir);
    if (!src.exists() || !src.isDirectory()) {
      throw new IOException(srcDir + " no such directory");
    }
    findSrcFiles(src, "");
  }

  private void findSrcFiles(File currentDir, String packageSoFar) {
    assert currentDir.isDirectory();
    File[] files = currentDir.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        findSrcFiles(file, packageSoFar + file.getName() + ".");
      } else if (file.isFile() && file.getName().endsWith(".java") && !file.getName().equals("package-info.java")){
        classes.add(packageSoFar + file.getName().substring(0, file.getName().length() - 5));
      }
    }
  }

  private void lookForAnnotations() throws ClassNotFoundException {
    for (String c : classes) {
      Class<?> instance = Class.forName(c);
      lookForDocOrEntry(instance);
      for (Class<?> subclass : instance.getClasses()) lookForDocOrEntry(subclass);

      for (Constructor<?> constructor : instance.getConstructors()) {
        addEntry(constructor.getAnnotation(DocumentEntry.class));
      }

      for (Method method : instance.getMethods()) {
        addEntry(method.getAnnotation(DocumentEntry.class));
      }

      for (Field field : instance.getFields()) {
        addEntry(field.getAnnotation(DocumentEntry.class));
      }
    }
  }

  private void lookForDocOrEntry(Class<?> clazz) {
    Document d = clazz.getAnnotation(Document.class);
    if (d != null) {
      docs.add(new Doc(d.name(), d.sections()));
    }
    addEntry(clazz.getAnnotation(DocumentEntry.class));
  }

  private void addEntry(DocumentEntry entry) {
    if (entry != null) {
      List<String> text = new ArrayList<>();
      Collections.addAll(text, entry.text());
      entries.merge(new StringPair(entry.documentName(), entry.documentSection()), text,
          (strings, strings2) -> {
            strings.addAll(strings2);
            return strings;
          });
    }
  }


  private void matchupDocsAndEntries() {
    for (Doc doc : docs) {
      for (Section section : doc.sections) {
        section.entries.addAll(entries.getOrDefault(new StringPair(doc.name, section.name), Collections.emptyList()));
      }
    }
  }

  private void writeOutDocs() throws IOException {
    File site = new File(targetDir);
    if (!site.exists() || !site.isDirectory()) {
      throw new IOException(targetDir + " no such directory");
    }
    for (Doc doc : docs) {
      // Open the file to write it to
      File fd = new File(site, doc.name + ".html");
      FileWriter writer = new FileWriter(fd);

      // Render the document into HTML
      Parser parser = Parser.builder().build();
      Node document = parser.parse(doc.toString());
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
   * @throws ClassNotFoundException if one of the classes found in the source tree cannot be loaded.
   */
  public static void main(String[] args) throws IOException, ClassNotFoundException {
    if (args.length != 2) {
      throw new IOException("Expects two arguments, the top level source directory, usually _project_/src/main/java" +
          "and the target site directory, usually _project_/target/site");
    }

    DocumentGenerator generator = new DocumentGenerator(args[0], args[1]);
    generator.generate();
  }

  // Represents a document that will be built, based on seeing a Document annotation
  private class Doc {
    final String name;
    final Section[] sections;

    Doc(String name, String[] sections) {
      this.name = name;
      this.sections = new Section[sections.length];
      for (int i = 0; i < sections.length; i++) this.sections[i] = new Section(sections[i]);
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      for (Section section : sections) {
        for (String entry : section.entries) {
          builder.append(entry).append("\n");
        }
      }
      return builder.toString();
    }
  }

  // Represents a section of a document that will be built, based on seeing a Document annotation
  private class Section {
    final String name;
    List<String> entries;

    Section(String name) {
      this.name = name;
      entries = new ArrayList<>();
    }
  }

  private class StringPair {
    final String docName;
    final String sectionName;

    StringPair(String docName, String sectionName) {
      this.docName = docName;
      this.sectionName = sectionName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      StringPair that = (StringPair) o;
      return Objects.equals(docName, that.docName) &&
          Objects.equals(sectionName, that.sectionName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(docName, sectionName);
    }
  }
}
