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

package de.sormuras.bach.project.structure;

import java.nio.file.Path;
import java.util.StringJoiner;

/** Common project-related paths. */
public /*static*/ final class Location {

  public static Location of() {
    return of(Path.of(""));
  }

  public static Location of(Path base) {
    return new Location(base, base.resolve(".bach/out"), base.resolve("lib"));
  }

  private final Path base;
  private final Path out;
  private final Path lib;

  public Location(Path base, Path out, Path lib) {
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

  @Override
  public String toString() {
    return new StringJoiner(", ", Location.class.getSimpleName() + "[", "]")
        .add("base=" + base)
        .add("out=" + out)
        .add("lib=" + lib)
        .toString();
  }

  public Path out(String first, String... more) {
    var path = Path.of(first, more);
    return out.resolve(path);
  }

  public Path classes(Realm realm) {
    return classes(realm, realm.release());
  }

  public Path classes(Realm realm, int release) {
    var version = "" + (release == 0 ? Runtime.version().feature() : release);
    return out.resolve("classes").resolve(version).resolve(realm.name());
  }

  public Path modules(Realm realm) {
    return out.resolve("modules").resolve(realm.name());
  }
}
