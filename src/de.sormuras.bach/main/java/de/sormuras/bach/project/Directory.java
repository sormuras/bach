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

package de.sormuras.bach.project;

import de.sormuras.bach.Convention;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** An optionally targeted directory of Java source files: {@code src/foo/main/java[-11]}. */
public /*static*/ class Directory {

  /** Directory kind. */
  public enum Type {
    UNKNOWN,
    SOURCE,
    SOURCE_WITH_ROOT_MODULE_DESCRIPTOR,
    RESOURCE;

    @Convention
    public static Type of(String string) {
      if (string.startsWith("java")) return SOURCE;
      if (string.contains("resource")) return RESOURCE;
      return UNKNOWN;
    }

    public boolean isSource() {
      return this == SOURCE || this == SOURCE_WITH_ROOT_MODULE_DESCRIPTOR;
    }

    public boolean isSourceWithRootModuleDescriptor() {
      return this == SOURCE_WITH_ROOT_MODULE_DESCRIPTOR;
    }

    public String toMarkdown() {
      return isSource() ? ":scroll:" : this == RESOURCE ? ":books:" : "?";
    }
  }

  /** Return directory instance for the given path. */
  public static Directory of(Path path) {
    var name = String.valueOf(path.getFileName());
    var type = Type.of(name);
    var release = javaReleaseFeatureNumber(name);
    return new Directory(path, type, release);
  }

  /** Return list of directories by scanning the given common root path: {@code src/foo/main}. */
  public static List<Directory> listOf(Path root) {
    if (Files.notExists(root)) return List.of();
    var directories = new ArrayList<Directory>();
    try (var stream = Files.newDirectoryStream(root, Files::isDirectory)) {
      stream.forEach(path -> directories.add(of(path)));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    directories.sort(Comparator.comparingInt(Directory::release));
    return List.copyOf(directories);
  }

  /** Return trailing integer part of {@code "java-{NUMBER}"} or zero. */
  @Convention
  static int javaReleaseFeatureNumber(String string) {
    if (string.endsWith("-module")) return 0;
    if (string.endsWith("-preview")) return Runtime.version().feature();
    if (string.startsWith("java-")) return Integer.parseInt(string.substring(5));
    return 0;
  }

  /** Return statistics summarizing over the passed {@code "java-{NUMBER}"} paths. */
  @Convention
  static IntSummaryStatistics javaReleaseStatistics(Stream<Path> paths) {
    var names = paths.map(Path::getFileName).map(Path::toString);
    return names.collect(Collectors.summarizingInt(Directory::javaReleaseFeatureNumber));
  }

  private final Path path;
  private final Type type;
  private final int release;

  public Directory(Path path, Type type, int release) {
    this.path = path;
    this.type = type;
    this.release = release;
  }

  public Path path() {
    return path;
  }

  public Type type() {
    return type;
  }

  public int release() {
    return release;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Directory.class.getSimpleName() + "[", "]")
        .add("path=" + path)
        .add("type=" + type)
        .add("release=" + release)
        .toString();
  }

  public String toMarkdown() {
    return type.toMarkdown() + " `" + path + "`" + (release == 0 ? "" : "@" + release);
  }
}
