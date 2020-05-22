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

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;

/** {@link Path}-related utilities. */
public /*static*/ class Paths {

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

  /** Test supplied path for pointing to a Java compilation unit. */
  public static boolean isJavaFile(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java");
  }

  /** Test supplied path for pointing to a Java module declaration compilation unit. */
  public static boolean isModuleInfoJavaFile(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().equals("module-info.java");
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
    var paths = new TreeSet<Path>();
    try (var directoryStream = Files.newDirectoryStream(directory, filter)) {
      directoryStream.forEach(paths::add);
    } catch (Exception e) {
      throw new Error("Stream directory '" + directory + "' failed: " + e, e);
    }
    return List.copyOf(paths);
  }

  private Paths() {}
}
