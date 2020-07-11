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

/** A {@code javac} call configuration. */
public final class Javac implements WithModuleSourceOptionsCall<Javac> {

  private final List<Argument> arguments;

  public Javac(List<Argument> arguments) {
    this.arguments = List.copyOf(arguments);
  }

  @Override
  public String name() {
    return "javac";
  }

  @Override
  public List<Argument> arguments() {
    return arguments;
  }

  @Override
  public String toDescriptiveLine() {
    var value = findValue("--module");
    if (value.isEmpty()) return WithModuleSourceOptionsCall.super.toDescriptiveLine();
    var modules = value.get().split(",");
    if (modules.length == 1) return "Compile module " + modules[0];
    return String.format("Compile %d modules: %s", modules.length, String.join(", ", modules));
  }

  @Override
  public Javac with(List<Argument> arguments) {
    if (arguments == arguments()) return this;
    return new Javac(arguments);
  }

  public Javac withRecommendedWarnings() {
    return with("-X" + "lint");
  }
}
