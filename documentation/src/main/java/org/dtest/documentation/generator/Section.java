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

class Section {
  static final String BEGIN = "In the beginning";

  private final String name;
  private String after;
  private String text;

  Section(String name) {
    this.name = name;
  }

  String getName() {
    return name;
  }

  String getAfter() {
    return after;
  }

  void setAfter(String after) {
    this.after = after;
  }

  void setText(String text) {
    this.text = text;
  }

  void buildDoc(StringBuilder builder) {
    assert after != null && text != null;
    builder.append(text);
  }
}
