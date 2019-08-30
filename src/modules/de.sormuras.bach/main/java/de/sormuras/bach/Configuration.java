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
public /*STATIC*/ final class Configuration {

  private static class ValidationError extends AssertionError {
    private ValidationError(String expected, Object hint) {
      super(String.format("expected that %s: %s", expected, hint));
    }
  }

  public static Configuration of() {
    var home = Path.of("");
    var work = Path.of("bin");
    return of(home, work);
  }

  public static Configuration of(Path home, Path work) {
    var lib = List.of(Path.of("lib"));
    var src = List.of(Path.of("src"));
    return new Configuration(home, work, lib, src);
  }

  private final Path home;
  private final Path work;
  private final List<Path> libraries;
  private final List<Path> sources;

  private Configuration(Path home, Path work, List<Path> libraries, List<Path> sources) {
    this.home = Objects.requireNonNull(home, "home must not be null");
    this.work = home(Objects.requireNonNull(work, "work must not be null"));
    this.libraries = home(requireNonEmpty(libraries, "libraries"));
    this.sources = home(requireNonEmpty(sources, "sources"));
  }

  private Path home(Path path) {
    return path.isAbsolute() ? path : home.resolve(path);
  }

  private List<Path> home(List<Path> paths) {
    return List.of(paths.stream().map(this::home).toArray(Path[]::new));
  }

  private static <C extends Collection<?>> C requireNonEmpty(C collection, String name) {
    if (Objects.requireNonNull(collection, name + " must not be null").isEmpty()) {
      throw new IllegalArgumentException(name + " must not be empty");
    }
    return collection;
  }

  final void validate() {
    requireDirectory(home);
    if (Util.list(home, Files::isDirectory).size() == 0)
      throw new ValidationError("home contains a directory", home.toUri());
    if (Files.exists(work)) {
      requireDirectory(work);
      if (!work.toFile().canWrite()) throw new ValidationError("bin is writable: %s", work.toUri());
    } else {
      var parentOfBin = work.toAbsolutePath().getParent();
      if (parentOfBin != null && !parentOfBin.toFile().canWrite())
        throw new ValidationError("parent of work is writable", parentOfBin.toUri());
    }
    requireDirectoryIfExists(getLibraryDirectory());
    getSourceDirectories().forEach(this::requireDirectory);
  }

  private void requireDirectoryIfExists(Path path) {
    if (Files.exists(path)) requireDirectory(path);
  }

  private void requireDirectory(Path path) {
    if (!Files.isDirectory(path))
      throw new ValidationError("path is a directory: %s", path.toUri());
  }

  public Path getHomeDirectory() {
    return home;
  }

  public Path getWorkspaceDirectory() {
    return work;
  }

  public Path getLibraryDirectory() {
    return libraries.get(0);
  }

  public List<Path> getLibraryPaths() {
    return libraries;
  }

  public List<Path> getSourceDirectories() {
    return sources;
  }

  @Override
  public String toString() {
    return "Configuration [" + String.join(", ", toStrings()) + "]";
  }

  public List<String> toStrings() {
    return List.of(
        String.format("home = '%s' -> %s", home, home.toUri()),
        String.format("workspace = '%s'", getWorkspaceDirectory()),
        String.format("library paths = %s", getLibraryPaths()),
        String.format("source directories = %s", getSourceDirectories()));
  }
}
