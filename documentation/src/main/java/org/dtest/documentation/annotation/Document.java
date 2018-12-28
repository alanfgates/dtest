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
package org.dtest.documentation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <code>Document</code> outlines a document that will be included in your documentation.  This annotation gives the layout of
 * the document and its location in the source tree.  It should be attached to a class object that needs to build
 * a user document above and beyond what can be done with standard Javadoc.
 * <p>
 * The layout of the document is determined by an array of document sections.  A document section is just a name,
 * which <code>DocumentEntry</code>s use to determine where they go in the document.
 * <p>
 * The format of the <code>DocumentEntry</code>s is markdown.  Ordering of entries within
 * a document section are not guaranteed, therefore it is recommended that features that require an order (such as
 * ordered lists) only be used within a <code>DocumentEntry</code> and not across.
 * <p>
 * These <code>DocumentEntry</code> annotations need not be in the same class as this
 * <code>Document</code>.  Each <code>DocumentEntry</code> indicates which <code>Document</code> it belongs to and which
 * section it should be placed in.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Document {

  /**
   * Name of the document.  This is used to connect the document to {@link DocumentEntry}s throughout the project.  This
   * will also determine the filename of the document in your source tree.
   * @return name of the document.
   */
  String name();

  /**
   * Sections of the document.  The names must be unique.  The order of the sections in the document is based on the
   * order in this array.
   * @return sections of the document.
   */
  String[] sections();

}
