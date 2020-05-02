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

package de.sormuras.bach.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;

/** {@link Path}-related utilities. */
public /*static*/ class Paths {
  /** Test for a regular file of size zero or an empty directory. */
  public static boolean isEmpty(Path path) {
    try {
      if (Files.isRegularFile(path)) return Files.size(path) == 0L;
      try (var stream = Files.list(path)) {
        return stream.findAny().isEmpty();
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Test supplied path for pointing to a Java module declaration compilation unit. */
  public static boolean isModuleInfoJavaFile(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().equals("module-info.java");
  }

  /** Walk all trees to find matching paths the given filter starting at given root paths. */
  public static List<Path> find(Collection<Path> roots, Predicate<Path> filter) {
    var files = new TreeSet<Path>();
    for (var root : roots) {
      try (var stream = Files.walk(root)) {
        stream.filter(filter).forEach(files::add);
      } catch (Exception e) {
        throw new Error("Walk directory '" + root + "' failed: " + e, e);
      }
    }
    return List.copyOf(files);
  }

  private Paths() {}
}
