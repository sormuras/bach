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

package de.sormuras.bach.internal;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** {@link Path}-related utilities. */
public /*static*/ class Paths {

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
      for (var path : selected.toArray(Path[]::new)) Files.deleteIfExists(path);
    } catch (Exception e) {
      throw new RuntimeException("Delete directory failed: " + directory, e);
    }
    return directory;
  }

  /** Convert path element names of the given unit into a reversed deque. */
  public static Deque<String> deque(Path path) {
    var deque = new ArrayDeque<String>();
    path.forEach(name -> deque.addFirst(name.toString()));
    return deque;
  }

  /** Test for a path pointing to a file system root like {@code /} or {@code C:\}. */
  public static boolean isRoot(Path path) {
    return path.toAbsolutePath().normalize().getNameCount() == 0;
  }

  /** Multi-release directory name pattern {@code java.?(\d+)}. */
  public static final Pattern JAVA_N_PATTERN = Pattern.compile("java.?(\\d+)");

  /** Return release feature number if directory's name matches, else an empty optional. */
  public static Optional<Integer> findMultiReleaseNumber(Path path) {
    var matcher = JAVA_N_PATTERN.matcher(name(path));
    if (!matcher.matches()) return Optional.empty();
    return Optional.of(Integer.parseInt(matcher.group(1)));
  }

  /** Test supplied path for pointing to a directory whose name matches {@link #JAVA_N_PATTERN}. */
  public static boolean isMultiReleaseDirectory(Path path) {
    return Files.isDirectory(path) && JAVA_N_PATTERN.matcher(name(path)).matches();
  }

  /** Return path's file name as a {@link String}. */
  public static String name(Path path) {
    return path.getNameCount() == 0 ? "" : path.getFileName().toString();
  }

  /** Test supplied path for pointing to a Java Archive file. */
  public static boolean isJarFile(Path path) {
    return Files.isRegularFile(path) && name(path).endsWith(".jar");
  }

  /** Test supplied path for pointing to a Java compilation unit. */
  public static boolean isJavaFile(Path path) {
    return Files.isRegularFile(path) && name(path).endsWith(".java");
  }

  /** Test supplied path for pointing to a Java module declaration compilation unit. */
  public static boolean isModuleInfoJavaFile(Path path) {
    return Files.isRegularFile(path) && name(path).equals("module-info.java");
  }

  /** Walk all trees to find matching paths the given filter starting at given root paths. */
  public static List<Path> find(Collection<Path> roots, int maxDepth, Predicate<Path> filter) {
    var paths = new TreeSet<Path>();
    for (var root : roots) {
      try (var stream = Files.walk(root, maxDepth)) {
        stream.filter(filter).forEach(paths::add);
      } catch (Exception e) {
        throw new Error("Walk directory '" + root + "' failed: " + e, e);
      }
    }
    return List.copyOf(paths);
  }

  /** List content of specified directory with the given filter applied in natural order. */
  public static List<Path> list(Path directory, DirectoryStream.Filter<? super Path> filter) {
    var paths = new TreeSet<>(Comparator.comparing(Path::toString));
    try (var directoryStream = Files.newDirectoryStream(directory, filter)) {
      directoryStream.forEach(paths::add);
    } catch (Exception e) {
      throw new Error("Stream directory '" + directory + "' failed: " + e, e);
    }
    return List.copyOf(paths);
  }

  private Paths() {}
}
