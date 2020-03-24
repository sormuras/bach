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

package de.sormuras.bach.api;

import java.nio.file.Path;

/** Common project-related paths. */
public /*static*/ final class Paths {

  private static final Path CLASSES = Path.of("classes");
  private static final Path MODULES = Path.of("modules");
  private static final Path SOURCES = Path.of("sources");
  private static final Path DOCUMENTATION = Path.of("documentation");
  private static final Path JAVADOC = DOCUMENTATION.resolve("javadoc");

  /** Create default instance for the specified base directory. */
  public static Paths of(Path base) {
    return new Paths(base, base.resolve(".bach"), base.resolve("lib"));
  }

  private final Path base;
  private final Path out;
  private final Path lib;

  public Paths(Path base, Path out, Path lib) {
    this.base = base;
    this.out = out;
    this.lib = lib;
  }

  public Path base() {
    return base;
  }

  public Path out() {
    return out;
  }

  public Path lib() {
    return lib;
  }

  public Path out(String first, String... more) {
    var path = Path.of(first, more);
    return out.resolve(path);
  }

  public Path classes(Realm realm) {
    return classes(realm, realm.feature());
  }

  public Path classes(Realm realm, int feature) {
    var version = "" + (feature == 0 ? "" : feature);
    return out.resolve(CLASSES).resolve(version).resolve(realm.name());
  }

  public Path javadoc() {
    return out.resolve(JAVADOC);
  }

  public Path modules(Realm realm) {
    return out.resolve(MODULES).resolve(realm.name());
  }

  public Path sources(Realm realm) {
    return out.resolve(SOURCES).resolve(realm.name());
  }
}
