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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class Doc {
  private final String name;
  private Map<String, Section> sections;
  private String lastSection; // the last added section

  Doc(String name) {
    this.name = name;
    sections = new HashMap<>();
    lastSection = Section.BEGIN;
  }

  String getName() {
    return name;
  }

  void addSection(Section section) throws IOException {
    Section prev = sections.put(section.getAfter(), section);
    if (prev != null) {
      throw new IOException("In document " + name + " found two sections both listed as after section " +
          section.getAfter() + ". " + section.getName() + " and " + prev.getName());
    }
    lastSection = section.getName();
  }

  String getLastSection() {
    return lastSection;
  }

  String buildDoc() throws IOException  {
    Section current = sections.remove(Section.BEGIN);
    if (current == null) throw new IOException("No beginning found for document " + name);
    StringBuilder builder = new StringBuilder();
    Section prev = null;
    while (current != null) {
      current.buildDoc(builder);
      prev = current;
      current = sections.remove(current.getName());
    }
    // Make sure we put every section in
    if (sections.size() > 0) {
      StringBuilder buf = new StringBuilder("Found a gap in the document ")
          .append(name)
          .append(".  No section was found immediately after ")
          .append(prev.getName())
          .append(" but sections ");
      sections.values().forEach(section -> buf.append(section.getName()).append(", "));
      buf.delete(buf.length() - 2, buf.length()); // remove the final ', '
      buf.append(" have not been placed in the document.");
      throw new IOException(buf.toString());
    }
    return builder.toString();
  }
}
