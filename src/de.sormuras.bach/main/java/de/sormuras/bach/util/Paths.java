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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** {@link Path}-related helpers. */
public class Paths {
  private Paths() {}

  /** Convenient short-cut to {@code "user.home"} as a path. */
  public static final Path USER_HOME = Path.of(System.getProperty("user.home"));

  /** Copy all files and directories from source to target directory. */
  public static void copy(Path source, Path target) throws Exception {
    copy(source, target, __ -> true);
  }

  /** Copy selected files and directories from source to target directory. */
  public static Set<Path> copy(Path source, Path target, Predicate<Path> filter) throws Exception {
    // debug("copy(source:`%s`, target:`%s`)%n", source, target);
    if (!Files.exists(source)) {
      throw new IllegalArgumentException("source must exist: " + source);
    }
    if (!Files.isDirectory(source)) {
      throw new IllegalArgumentException("source must be a directory: " + source);
    }
    if (Files.exists(target)) {
      if (!Files.isDirectory(target)) {
        throw new IllegalArgumentException("target must be a directory: " + target);
      }
      if (target.equals(source)) {
        return Set.of();
      }
      if (target.startsWith(source)) {
        // copy "a/" to "a/b/"...
        throw new IllegalArgumentException("target must not a child of source");
      }
    }
    var paths = new TreeSet<Path>();
    try (var stream = Files.walk(source).sorted()) {
      for (var path : stream.collect(Collectors.toList())) {
        var destination = target.resolve(source.relativize(path));
        if (Files.isDirectory(path)) {
          Files.createDirectories(destination);
          continue;
        }
        if (filter.test(path)) {
          Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
          paths.add(destination);
        }
      }
    }
    return paths;
  }

  public static Path createDirectories(Path directory) {
    try {
      Files.createDirectories(directory);
    } catch (Exception e) {
      throw new RuntimeException("Create directories failed: " + directory, e);
    }
    return directory;
  }

  public static Path deleteIfExists(Path directory) {
    if (Files.notExists(directory)) return directory;
    return delete(directory, __ -> true);
  }

  public static Path delete(Path directory) {
    return delete(directory, __ -> true);
  }

  public static Path delete(Path directory, Predicate<Path> filter) {
    // trivial case: delete existing empty directory or single file
    try {
      Files.deleteIfExists(directory);
      return directory;
    } catch (DirectoryNotEmptyException ignored) {
      // fall-through
    } catch (Exception e) {
      throw new RuntimeException("Delete directory failed: " + directory, e);
    }
    // default case: walk the tree...
    try (var stream = Files.walk(directory)) {
      var selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
      for (var path : selected.collect(Collectors.toList())) {
        Files.deleteIfExists(path);
      }
    } catch (Exception e) {
      throw new RuntimeException("Delete directory failed: " + directory, e);
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

  public static String name(Path path, String defaultName) {
    var name = path.toAbsolutePath().getFileName();
    return name != null ? name.toString() : defaultName;
  }

  public static String readString(Path path) {
    try {
      return Files.readString(path);
    } catch (Exception e) {
      throw new RuntimeException("Read all content from file failed: " + path, e);
    }
  }

  /** Convert path to string and replace first element named {@code key} with a {@code '*'}. */
  public static String star(Path path, String key) {
    return toString(path, key, true, "*");
  }

  /** Convert path to string. */
  public static String toString(Path path, String key, boolean consume, String... replacements) {
    var deque = new ArrayDeque<>(List.of(replacements));
    var strings = new ArrayList<String>();
    for (var element : path) {
      var string = element.toString();
      if (string.equals("module-info.java")) break;
      if (!deque.isEmpty() && string.equals(key)) {
        strings.add(consume ? deque.pop() : deque.peek());
      } else {
        strings.add(string);
      }
    }
    return String.join(File.separator, strings);
  }

  /** Walk directory tree structure. */
  public static void walk(Path root, Consumer<String> out) {
    try (var stream = Files.walk(root)) {
      stream
          .map(root::relativize)
          .map(path -> path.toString().replace('\\', '/'))
          .sorted()
          .filter(Predicate.not(String::isEmpty))
          .forEach(out);
    } catch (Exception e) {
      throw new RuntimeException("Walking tree failed: " + root, e);
    }
  }
}
