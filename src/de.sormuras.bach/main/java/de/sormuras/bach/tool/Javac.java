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
import java.util.Map;

/** A {@code javac} tool call configuration. */
public final class Javac implements Call<Javac> {

  public static Javac of() {
    return new Javac(List.of());
  }

  private final List<Argument> arguments;

  public Javac(List<Argument> arguments) {
    this.arguments = arguments;
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
  public Javac with(List<Argument> arguments) {
    return new Javac(arguments);
  }

  public Javac withTerminateCompilationIfWarningsOccur() {
    return with("-W" + "error");
  }

  public Javac withCompileForJavaRelease(int release) {
    return with("--release", release);
  }

  public Javac withRecommendedWarnings() {
    return with("-X" + "lint");
  }

  public Javac withModuleSourcePaths(Iterable<String> moduleSourcePaths) {
    return with(moduleSourcePaths, Javac::withModuleSourcePath);
  }

  public Javac withModuleSourcePath(String moduleSourcePath) {
    return with("--module-source-path", moduleSourcePath);
  }

  public Javac withPatchModules(Map<String, String> patches) {
    return with(patches.entrySet(), Javac::withPatchModule);
  }

  private Javac withPatchModule(Map.Entry<String, String> patch) {
    return withPatchModule(patch.getKey(), patch.getValue());
  }

  public Javac withPatchModule(String module, String path) {
    return with("--patch-module", module + '=' + path);
  }

  public Javac withWarnings(String key, String... more) {
    var moreKeys = more.length == 0 ? "" : ',' + String.join(",", more);
    return with("-X" + "lint:" + key + moreKeys);
  }
}
