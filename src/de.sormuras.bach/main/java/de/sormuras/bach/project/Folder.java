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

  public static Folder of() {
    return of(Path.of(""));
  }

  public static Folder of(Path base) {
    return new Folder(base, base.resolve("src"), base.resolve("lib"), base.resolve(".bach/out"));
  }

  private final Path base;
  private final Path src;
  private final Path lib;
  private final Path out;
  private final Path log;

  public Folder(Path base, Path src, Path lib, Path out) {
    this.base = base;
    this.src = src;
    this.lib = lib;
    this.out = out;
    this.log = out.resolve("log");
  }

  static Path resolve(Path path, String... more) {
    if (more.length == 0) return path;
    return path.resolve(String.join(File.separator, more));
  }

  public Path base() {
    return base;
  }

  public Path out() {
    return out;
  }

  public Path out(String... more) {
    return resolve(out, more);
  }

  public Path lib() {
    return lib;
  }

  public Path src() {
    return src;
  }

  public Path src(String... more) {
    return resolve(src, more);
  }

  public Path log() {
    return log;
  }

  public Path log(String... more) {
    return resolve(log, more);
  }

  public Path realm(String realm, String... more) {
    return resolve(out.resolve(realm), more);
  }

  public Path modules(String realm, String... more) {
    return resolve(realm(realm,"modules"), more);
  }
}
