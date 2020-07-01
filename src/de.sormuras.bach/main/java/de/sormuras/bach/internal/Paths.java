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

import java.io.File;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** {@link Path}-related utilities. */
public final class Paths {

  public static Path createDirectories(Path directory) {
    try {
      return Files.createDirectories(directory);
    } catch (Exception e) {
      throw new RuntimeException("Create directories failed: " + directory, e);
    }
  }

  public static Path deleteDirectories(Path directory) {
    return deleteDirectories(directory, __ -> true);
  }

  public static Path deleteDirectories(Path directory, Predicate<Path> filter) {
    // trivial case: delete existing empty directory or single file
    try {
      Files.deleteIfExists(directory);
      return directory;
    } catch (DirectoryNotEmptyException ignored) {
      // fall-through
    } catch (Exception e) {
      throw new RuntimeException("Delete directories failed: " + directory, e);
    }
    // default case: walk the tree...
    try (var stream = Files.walk(directory)) {
      var selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
      for (var path : selected.toArray(Path[]::new)) Files.deleteIfExists(path);
    } catch (Exception e) {
      throw new RuntimeException("Delete directories failed: " + directory, e);
    }
    return directory;
  }

  /** Convert path element names of the given unit into a reversed deque. */
  public static Deque<String> deque(Path path) {
    var deque = new ArrayDeque<String>();
    path.forEach(name -> deque.addFirst(name.toString()));
    return deque;
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
    return isJavaFile(path) && name(path).equals("module-info.java");
  }

  /** Test supplied path for pointing to a Java module declaration for a given realm. */
  public static boolean isModuleInfoJavaFileForRealm(Path info, String realm) {
    return isModuleInfoJavaFile(info) && Collections.frequency(deque(info), realm) == 1;
  }

  /** Return {@code true} if the given name of the view is supported by the given file. */
  public static boolean isViewSupported(Path file, String view) {
    return file.getFileSystem().supportedFileAttributeViews().contains(view);
  }

  /** List content of specified directory in natural order with the given filter applied. */
  public static List<Path> list(Path directory, DirectoryStream.Filter<? super Path> filter) {
    var paths = new TreeSet<>(Comparator.comparing(Path::toString));
    try (var directoryStream = Files.newDirectoryStream(directory, filter)) {
      directoryStream.forEach(paths::add);
    } catch (Exception e) {
      throw new Error("Stream directory '" + directory + "' failed: " + e, e);
    }
    return List.copyOf(paths);
  }

  /** Join a collection of path objects to a string using the system-dependent separator. */
  public static String join(Collection<Path> paths) {
    return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
  }

  /** Return path's file name as a {@link String}. */
  public static String name(Path path) {
    return path.getNameCount() == 0 ? "" : path.getFileName().toString();
  }

  public static String replaceBackslashes(Path path) {
    return path.toString().replace('\\', '/');
  }

  public static String quote(Path path) {
    return '"' + replaceBackslashes(path) + '"';
  }

  /** Return the size of a file in bytes. */
  public static long size(Path path) {
    try {
      return Files.size(path);
    } catch (Exception e) {
      throw new Error("Size of file failed: " + e, e);
    }
  }

  private Paths() {}
}
