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
import java.util.List;

/*BODY*/
public /*STATIC*/ class Configuration {

  public static Configuration of() {
    return of(Path.of(""));
  }

  public static Configuration of(Path home) {
    return of(home, Path.of("bin"), Path.of("lib"), Path.of("src"));
  }

  public static Configuration of(Path home, Path work, Path lib, Path src) {
    return new Configuration(
        Util.requireNonNull(home, "home directory"),
        resolve(home, work, "workspace directory"),
        resolve(home, List.of(lib), "library paths"),
        resolve(home, List.of(src), "source directories"));
  }

  private static Path resolve(Path home, Path path, String name) {
    return Util.requireNonNull(path, name).isAbsolute() ? path : home.resolve(path);
  }

  private static List<Path> resolve(Path home, List<Path> paths, String name) {
    return List.of(
        Util.requireNonNull(paths, name).stream()
            .map(path -> resolve(home, path, "element of " + name))
            .toArray(Path[]::new));
  }

  private final Path homeDirectory;
  private final Path workspaceDirectory;
  private final List<Path> libraryPaths;
  private final List<Path> sourceDirectories;

  private Configuration(
      Path homeDirectory,
      Path workspaceDirectory,
      List<Path> libraryPaths,
      List<Path> sourceDirectories) {
    this.homeDirectory = homeDirectory;
    this.workspaceDirectory = workspaceDirectory;
    this.libraryPaths = Util.requireNonEmpty(libraryPaths, "library paths");
    this.sourceDirectories = Util.requireNonEmpty(sourceDirectories, "source directories");
  }

  public Path getHomeDirectory() {
    return homeDirectory;
  }

  public Path getWorkspaceDirectory() {
    return workspaceDirectory;
  }

  public Path getLibraryDirectory() {
    return getLibraryPaths().get(0);
  }

  public List<Path> getLibraryPaths() {
    return libraryPaths;
  }

  public List<Path> getSourceDirectories() {
    return sourceDirectories;
  }

  @Override
  public String toString() {
    return "Configuration [" + String.join(", ", toStrings()) + "]";
  }

  public List<String> toStrings() {
    var home = getHomeDirectory();
    return List.of(
        String.format("home = '%s' -> %s", home, home.toUri()),
        String.format("workspace = '%s'", getWorkspaceDirectory()),
        String.format("library paths = %s", getLibraryPaths()),
        String.format("source directories = %s", getSourceDirectories()));
  }

  static class ValidationError extends AssertionError {
    private ValidationError(String expected, Object hint) {
      super(String.format("expected that %s: %s", expected, hint));
    }
  }

  static void validate(Configuration configuration) {
    var home = configuration.getHomeDirectory();
    validateDirectory(home);
    if (Util.list(home, Files::isDirectory).size() == 0)
      throw new ValidationError("home contains a directory", home.toUri());
    var work = configuration.getWorkspaceDirectory();
    if (Files.exists(work)) {
      validateDirectory(work);
      if (!work.toFile().canWrite()) throw new ValidationError("bin is writable: %s", work.toUri());
    } else {
      var parentOfBin = work.toAbsolutePath().getParent();
      if (parentOfBin != null && !parentOfBin.toFile().canWrite())
        throw new ValidationError("parent of work is writable", parentOfBin.toUri());
    }
    validateDirectoryIfExists(configuration.getLibraryDirectory());
    configuration.getSourceDirectories().forEach(Configuration::validateDirectory);
  }

  static void validateDirectoryIfExists(Path path) {
    if (Files.exists(path)) validateDirectory(path);
  }

  static void validateDirectory(Path path) {
    if (!Files.isDirectory(path))
      throw new ValidationError("path is a directory", path.toUri());
  }
}
