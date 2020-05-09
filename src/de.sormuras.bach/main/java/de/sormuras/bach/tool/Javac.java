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

import java.util.Set;

/** A call to {@code javac}, the Java compiler. */
public /*static*/ class Javac extends AbstractTool {

  private Set<String> compileModulesCheckingTimestamps = Set.of();

  public Javac() {
    super("javac");
  }

  @Override
  protected void arguments(Arguments arguments) {
    var module = getCompileModulesCheckingTimestamps();
    if (assigned(module)) arguments.add("--module", String.join(",", module));
  }

  public Set<String> getCompileModulesCheckingTimestamps() {
    return compileModulesCheckingTimestamps;
  }

  public Javac setCompileModulesCheckingTimestamps(Set<String> moduleNames) {
    this.compileModulesCheckingTimestamps = moduleNames;
    return this;
  }
}
