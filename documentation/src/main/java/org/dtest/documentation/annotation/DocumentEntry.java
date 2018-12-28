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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A comment to be placed in a {@link Document}.  This comment must identify the name and section of the
 * <code>Document</code> that it belong in.  If it is an element of the list, there is no control over where
 * in the list it is placed.  @Target is intentionally not used here so that any type of entity can be annotated
 * using a Comment.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Comment {

  /**
   * Name of the document this comment should be placed in.
   * @return document name.
   */
  String documentName();

  /**
   * Section of the document this comment should be placed in.
   * @return document section.
   */
  String documentSection();

  /**
   * Text that makes up the comment.  Each string will be placed in a separate paragraph, in the order provided.
   * @return text of the comment
   */
  String[] text();
}
