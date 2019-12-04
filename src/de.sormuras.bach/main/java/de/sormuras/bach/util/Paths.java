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

package de.sormuras.bach.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** {@link Path}-related helpers. */
public class Paths {
  private Paths() {}

  /** Convenient short-cut to {@code "user.home"} as a path. */
  public static final Path USER_HOME = Path.of(System.getProperty("user.home"));

  public static Path createDirectories(Path directory) {
    try {
      Files.createDirectories(directory);
    } catch (Exception e) {
      throw new RuntimeException("Create directories failed: " + directory, e);
    }
    return directory;
  }

  public static List<Path> filter(List<Path> paths, Predicate<Path> filter) {
    return paths.stream().filter(filter).collect(Collectors.toList());
  }

  public static List<Path> filterExisting(List<Path> paths) {
    return filter(paths, Files::exists);
  }

  /** Walk all trees to find matching paths the given filter starting at given root paths. */
  public static List<Path> find(Collection<Path> roots, Predicate<Path> filter) {
    var files = new TreeSet<Path>();
    for (var root : roots) {
      try (var stream = Files.walk(root)) {
        stream.filter(filter).forEach(files::add);
      } catch (Exception e) {
        throw new Error("Walking directory '" + root + "' failed: " + e, e);
      }
    }
    return List.copyOf(files);
  }

  /** Test supplied path for pointing to a Java source compilation unit. */
  public static boolean isJavaFile(Path path) {
    if (Files.isRegularFile(path)) {
      var name = path.getFileName().toString();
      if (name.endsWith(".java")) {
        return name.indexOf('.') == name.length() - 5; // single dot in filename
      }
    }
    return false;
  }

  /** Test supplied path for pointing to a Java module declaration compilation unit. */
  public static boolean isModuleFile(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().equals("module-info.java");
  }

  /** Test supplied path for pointing to a {@code J}ava {@code AR}chive file. */
  public static boolean isJarFile(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar");
  }

  public static String join(List<Path> paths) {
    return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
  }

  public static Properties load(Properties properties, Path path) {
    if (Files.isRegularFile(path)) {
      try (var reader = Files.newBufferedReader(path)) {
        properties.load(reader);
      } catch (Exception e) {
        throw new RuntimeException("Load properties failed: " + path, e);
      }
    }
    return properties;
  }

  public static List<Path> list(Path directory, Predicate<Path> filter) {
    try (var stream = Files.list(directory)) {
      return stream.filter(filter).sorted().collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("List directory failed: " + directory, e);
    }
  }

  public static List<Path> list(Path directory, String glob) {
    try (var items = Files.newDirectoryStream(directory, glob)) {
      return StreamSupport.stream(items.spliterator(), false).sorted().collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("List directory using glob failed: " + directory, e);
    }
  }

  public static String readString(Path path) {
    try {
      return Files.readString(path);
    } catch (Exception e) {
      throw new RuntimeException("Read all content from file failed: " + path, e);
    }
  }
}
