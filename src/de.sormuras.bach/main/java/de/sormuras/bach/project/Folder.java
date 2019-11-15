/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

package de.sormuras.bach.project;

import java.io.File;
import java.nio.file.Path;

public /*record*/ class Folder {
  private final Path base;

  public Folder(Path base) {
    this.base = base;
  }

  static Path resolve(Path path, String... more) {
    if (more.length == 0) return path;
    return path.resolve(String.join(File.separator, more));
  }

  public Path base() {
    return base;
  }

  public Path out(String... more) {
    return resolve(base.resolve(".bach/out"), more);
  }

  public Path lib(String... more) {
    return resolve(base.resolve("lib"), more);
  }

  public Path log(String... more) {
    return resolve(out().resolve("log"), more);
  }

  public Path realm(String realm, String... more) {
    return resolve(out().resolve(realm), more);
  }

  public Path modules(String realm, String... more) {
    return resolve(realm(realm).resolve("modules"), more);
  }
}
