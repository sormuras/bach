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

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.StringJoiner;

/** Well-known paths. */
public /*static*/ final class Workspace {

  public static Workspace of() {
    return of(Path.of(""));
  }

  public static Workspace of(Path base) {
    return new Workspace(base, base.resolve("lib"), base.resolve(".bach/workspace"));
  }

  private final Path base;
  private final Path lib;
  private final Path workspace;

  public Workspace(Path base, Path lib, Path workspace) {
    this.base = base;
    this.lib = lib;
    this.workspace = workspace;
  }

  public Path base() {
    return base;
  }

  public Path lib() {
    return lib;
  }

  public Path workspace() {
    return workspace;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Workspace.class.getSimpleName() + "[", "]")
        .add("base=" + base)
        .add("lib=" + lib)
        .add("workspace=" + workspace)
        .toString();
  }

  public Path workspace(String first, String... more) {
    return workspace.resolve(Path.of(first, more));
  }

  public Path classes(String realm, int release) {
    var version = String.valueOf(release == 0 ? Runtime.version().feature() : release);
    return workspace("classes", realm, version);
  }

  public Path modules(String realm) {
    return workspace("modules", realm);
  }

  public Path module(String realm, String module, Version version) {
    return modules(realm).resolve(jarFileName(module, version, ""));
  }

  public String jarFileName(String module, Version version, String classifier) {
    var versionSuffix = version == null ? "" : "-" + version;
    var classifierSuffix = classifier == null || classifier.isEmpty() ? "" : "-" + classifier;
    return module + versionSuffix + classifierSuffix + ".jar";
  }
}
