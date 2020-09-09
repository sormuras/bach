/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach;

/** A tool call object with its name and an array of argument strings. */
public interface ToolCall {
  /**
   * Return the name of the tool to be called.
   *
   * @apiNote It is recommended that the name be the same as would be used on the command line: for
   *     example, {@code "javac"}, {@code "jar"}, {@code "jlink"}.
   * @return a non-empty string representing the name of the tool to be called
   */
  String name();

  /**
   * Return the possibly empty arguments of this tool call.
   *
   * @return an array of strings
   */
  String[] args();
}
