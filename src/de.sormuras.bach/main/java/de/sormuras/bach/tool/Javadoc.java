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
import java.util.function.UnaryOperator;

/** A {@code javadoc} call configuration. */
public final class Javadoc implements WithModuleSourceOptionsCall<Javadoc> {

  @FunctionalInterface
  public interface Tweak extends UnaryOperator<Javadoc> {}

  private final List<Argument> arguments;

  public Javadoc(List<Argument> arguments) {
    this.arguments = List.copyOf(arguments);
  }

  @Override
  public String name() {
    return "javadoc";
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
    if (modules.length == 1) return "Generate API documentation for module " + modules[0];
    return String.format(
        "Generate API documentation for %d modules: %s",
        modules.length, String.join(", ", modules));
  }

  @Override
  public Javadoc with(List<Argument> arguments) {
    if (arguments == arguments()) return this;
    return new Javadoc(arguments);
  }

  public Javadoc withRecommendedWarnings() {
    return with("-Xdoclint");
  }
}
