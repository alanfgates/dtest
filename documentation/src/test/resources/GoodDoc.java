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
public class GoodDoc {

  /*~~
   * @document good
   * @section intro
   * # Introduction
   * This is the introduction.
   * It is very well written.
   *
   */

  private static final String SOMETHING = something;

  /**
   * A constructor
   */
  public GoodDoc() {
  }

  /*~~
   * @document good
   * @section body
   * @after intro
   * This is the body.  It is even better written than the introdution.
   *
   * *Something bold*
   *
   * A list
   * - one
   * - two
   * - three
   *
   * A [link](www.hortonworks.com)
   *
   * It is super interesting.
   *
   */
  /**
   * A method
   * @returns a string
   */
  public String myMethod() {
    return "";
  }

  /*~~
   * @document evenbetter
   * @section first
   * @begin
   * This is a second document, inserted to make sure the parser can handle multiple documents.
   *
   */

  /*~~
   * Empty comment with no tags.  This should not be included.
   */

  /*~~
   * @document evenbetter
   * @section second
   * This is a comment with no after, to make sure things are ordered properly.
   */

  /*~~
   * ## Conclusion
   * This is the conclusion.  I purposefully inverted the tag order here to see what happens.
   * @after body
   * @section conclusion
   * @document good
   */
  public String anotherMethod() {
    return "";
  }

}
