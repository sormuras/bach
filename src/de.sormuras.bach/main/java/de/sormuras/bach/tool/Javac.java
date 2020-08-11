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
    var moduleValue = findValue("--module");
    if (moduleValue.isPresent()) {
      var modules = moduleValue.get().split(",");
      if (modules.length == 1) return "Compile module " + modules[0];
      var list = String.join(", ", modules);
      return String.format("Compile multiple modules: %s", list);
    }
    var destinationDirectory = findValue("-d");
    if (destinationDirectory.isPresent()) {
      var directory = destinationDirectory.get();
      var options = arguments.stream().map(Argument::option);
      var files = options.filter(option -> option.endsWith(".java")).toArray(String[]::new);
      if (files.length == 1) return "Compile " + files[0] + " to " + directory;
      return "Compile " + files.length + " source files to " + directory;
    }
    return WithModuleSourceOptionsCall.super.toDescriptiveLine();
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
