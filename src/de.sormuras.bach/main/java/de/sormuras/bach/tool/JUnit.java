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

import de.sormuras.bach.Call;
import de.sormuras.bach.internal.Modules;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;

/** A JUnit Platform launcher configuration. */
public final class JUnit implements Call<JUnit> {

  @FunctionalInterface
  public interface Tweak extends UnaryOperator<JUnit> {}

  private final String module;
  private final List<Path> modulePaths;
  private final List<Argument> arguments;

  public JUnit(String module, List<Path> modulePaths, List<Argument> arguments) {
    this.module = module;
    this.modulePaths = modulePaths;
    this.arguments = arguments;
  }

  @Override
  public String name() {
    return "junit";
  }

  @Override
  public List<Argument> arguments() {
    return arguments;
  }

  @Override
  public Optional<ToolProvider> findProvider() {
    return Modules.findTools(module, modulePaths).stream()
        .filter(provider -> provider.name().equals(name()))
        .findAny();
  }

  @Override
  public String toDescriptiveLine() {
    var value = findValue("--select-module");
    if (value.isEmpty()) return Call.super.toDescriptiveLine();
    return "Launch JUnit Platform for module " + value.get();
  }

  @Override
  public JUnit with(List<Argument> arguments) {
    return new JUnit(module, modulePaths, arguments);
  }
}
