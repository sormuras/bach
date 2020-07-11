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
import java.util.spi.ToolProvider;

/** A test module, named {@code test("${MODULE}")}, call configuration. */
public final class TestModule implements Call<TestModule> {

  private final String module;
  private final List<Path> modulePaths;

  public TestModule(String module, List<Path> modulePaths) {
    this.module = module;
    this.modulePaths = modulePaths;
  }

  @Override
  public String name() {
    return "test";
  }

  @Override
  public List<Argument> arguments() {
    return List.of(Argument.of(module));
  }

  @Override
  public Optional<ToolProvider> findProvider() {
    var requiredProviderName = "test(" + module + ")";
    return Modules.findTools(module, modulePaths).stream()
        .filter(provider -> provider.name().equals(requiredProviderName))
        .findAny();
  }

  @Override
  public String toDescriptiveLine() {
    return "Launch tests provided via test(" + module + ")";
  }

  @Override
  public TestModule with(List<Argument> arguments) {
    return this;
  }
}
