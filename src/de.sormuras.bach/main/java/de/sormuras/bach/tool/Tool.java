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

import java.util.List;

/** An arbitrary tool call configuration. */
public final class Tool implements Call<Tool> {

  public static Tool of(String name) {
    return new Tool(name, List.of());
  }

  private final String name;
  private final List<Argument> arguments;

  public Tool(String name, List<Argument> arguments) {
    this.name = name;
    this.arguments = arguments;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public List<Argument> arguments() {
    return arguments;
  }

  @Override
  public Tool with(List<Argument> arguments) {
    return new Tool(name, arguments);
  }
}
