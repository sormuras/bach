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

package de.sormuras.bach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/*BODY*/
public /*STATIC*/ class Configuration {

  public static Configuration of() {
    var home = Path.of("");
    var bin = Path.of("bin");
    return of(home, bin);
  }

  public static Configuration of(Path home, Path bin) {
    var lib = List.of(Path.of("lib"));
    var src = List.of(Path.of("src"));
    return new Configuration(home, bin, lib, src);
  }

  private final Path home;
  private final Path bin;
  private final List<Path> lib;
  private final List<Path> src;

  private Configuration(Path home, Path bin, List<Path> lib, List<Path> src) {
    this.home = Objects.requireNonNull(home, "home must not be null");
    this.bin = home(Objects.requireNonNull(bin, "bin must not be null"));
    this.lib = List.of(requireNonEmpty(lib).stream().map(this::home).toArray(Path[]::new));
    this.src = List.of(requireNonEmpty(src).stream().map(this::home).toArray(Path[]::new));
  }

  private static <C extends Collection<?>> C requireNonEmpty(C collection) {
    if (collection.isEmpty()) {
      throw new IllegalArgumentException("collection must not be empty");
    }
    return collection;
  }

  private Path home(Path path) {
    return path.isAbsolute() ? path : home.resolve(path);
  }

  public static class Error extends AssertionError {
    private Error(String expected, Object hint) {
      super(String.format("expected that %s: %s", expected, hint));
    }
  }

  final void validate() {
    requireDirectory(home);
    if (Util.list(home, Files::isDirectory).size() == 0)
      throw new Error("home contains a directory", home.toUri());
    if (Files.exists(bin)) {
      requireDirectory(bin);
      if (!bin.toFile().canWrite()) throw new Error("bin is writable: %s", bin.toUri());
    } else {
      var parentOfBin = bin.toAbsolutePath().getParent();
      if (parentOfBin != null && !parentOfBin.toFile().canWrite())
        throw new Error("parent of work is writable", parentOfBin.toUri());
    }
    requireDirectoryIfExists(getLibraryDirectory());
    getSourceDirectories().forEach(this::requireDirectory);
  }

  private void requireDirectoryIfExists(Path path) {
    if (Files.exists(path)) requireDirectory(path);
  }

  private void requireDirectory(Path path) {
    if (!Files.isDirectory(path)) throw new Error("path is a directory: %s", path.toUri());
  }

  public Path getHomeDirectory() {
    return home;
  }

  public Path getWorkspaceDirectory() {
    return bin;
  }

  public Path getLibraryDirectory() {
    return lib.get(0);
  }

  public List<Path> getLibraryDirectories() {
    return lib;
  }

  public List<Path> getSourceDirectories() {
    return src;
  }

  @Override
  public String toString() {
    return "Configuration [" + String.join(", ", toStrings()) + "]";
  }

  public List<String> toStrings() {
    return List.of(
        String.format("home = '%s' -> %s", home, home.toUri()),
        String.format("bin = '%s'", bin),
        String.format("lib = %s", lib),
        String.format("src = %s", src));
  }
}
