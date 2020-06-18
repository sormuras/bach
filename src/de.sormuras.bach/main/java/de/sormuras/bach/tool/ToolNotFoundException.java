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

package de.sormuras.bach.tool;

/** Signals that a named tool couldn't be found. */
public class ToolNotFoundException extends IllegalStateException {

  private static final long serialVersionUID = 1471606379434991753L;

  /** Initialize an exception instance with the tool name included in the detail message. */
  public ToolNotFoundException(String name) {
    super("Tool with name '" + name + "' not found");
  }
}
