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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class DocParser {
  private static final String COMMENT_BEGIN = "/*~~";
  private static final String COMMENT_END = "*/";
  private static final String DOC_TAG = "@document";
  private static final String SECTION_TAG = "@section";
  private static final String AFTER_TAG = "@after";
  private static final String BEGIN_TAG = "@begin";

  private enum ParserState { NOT_IN_COMMENT, IN_COMMENT}

  private Map<String, Doc> docs;
  private Doc currentDoc;
  private Section currentSection;
  private String currentAfter;
  private StringBuilder currentText;
  private ParserState state;

  DocParser() {
    docs = new HashMap<>();
  }

  void parseFile(File input) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(input))) {
      String line = null;
      int lineNum = 0;
      resetState();
      while ((line = reader.readLine()) != null) {
        lineNum++;
        line = line.trim();
        if (state == ParserState.NOT_IN_COMMENT) {
          if (line.startsWith(COMMENT_BEGIN)) {
            state = ParserState.IN_COMMENT;
          }
        } else if (state == ParserState.IN_COMMENT) {
          if (line.startsWith(COMMENT_END)) {
            if (currentDoc != null && currentSection != null) {
              // Check to see if this section has an after, set to the last seen section for the document (which
              // could be the beginning if we haven't seen the document before).
              if (currentAfter == null) currentSection.setAfter(currentDoc.getLastSection());
              else currentSection.setAfter(currentAfter);
              currentSection.setText(currentText.toString());
              currentDoc.addSection(currentSection);
            }
            resetState();
          } else {
            // Remove the leading '* ' from the comment line.  We have to take this carefully as the line might be
            // empty other than the *.
            assert line.startsWith("*") : "line number " + lineNum;
            line = line.substring(1);
            if (line.startsWith(" ")) line = line.substring(1);
            if (line.trim().startsWith(DOC_TAG)) {
              String docName = line.trim().substring(DOC_TAG.length()).trim();
              if (currentDoc != null) {
                throw new IOException("Each section must have only one @document tag.");
              }
              currentDoc = docs.computeIfAbsent(docName, Doc::new);
            } else if (line.trim().startsWith(SECTION_TAG)) {
              String sectionName = line.trim().substring(SECTION_TAG.length()).trim();
              if (currentSection != null) {
                throw new IOException("Each section must have only one @section tag.");
              }
              currentSection = new Section(sectionName);
            } else if (line.trim().startsWith(AFTER_TAG)) {
              if (currentAfter != null) {
                throw new IOException("Multiple @after or both @after and @begin tags not allowed in a single section");
              }
              currentAfter = line.trim().substring(AFTER_TAG.length()).trim();
            } else if (line.trim().startsWith(BEGIN_TAG)) {
              if (currentAfter != null) {
                throw new IOException("Both @begin and @after tags or multiple @begin tags not allowed in a single section");
              }
              currentAfter = Section.BEGIN;
            } else {
              currentText.append(line).append("\n");
            }
          }
        }
      }
    }
  }

  Collection<Doc> getDocs() {
    return docs.values();
  }

  private void resetState() {
    state = ParserState.NOT_IN_COMMENT;
    currentDoc = null;
    currentSection = null;
    currentAfter = null;
    currentText = new StringBuilder();
  }

}
