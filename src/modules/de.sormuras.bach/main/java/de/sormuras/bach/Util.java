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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*BODY*/
/** Static helpers. */
/*STATIC*/ class Util {

  static <E extends Comparable<E>> Set<E> concat(Set<E> one, Set<E> two) {
    return Stream.concat(one.stream(), two.stream()).collect(Collectors.toCollection(TreeSet::new));
  }

  static Optional<Method> findApiMethod(Class<?> container, String name) {
    try {
      var method = container.getMethod(name);
      return isApiMethod(method) ? Optional.of(method) : Optional.empty();
    } catch (NoSuchMethodException e) {
      return Optional.empty();
    }
  }

  static List<Path> findExistingDirectories(List<Path> directories) {
    return directories.stream().filter(Files::isDirectory).collect(Collectors.toList());
  }

  static boolean isApiMethod(Method method) {
    if (method.getDeclaringClass().equals(Object.class)) return false;
    if (Modifier.isStatic(method.getModifiers())) return false;
    return method.getParameterCount() == 0;
  }

  static boolean isModuleInfo(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().equals("module-info.java");
  }

  static List<Path> list(Path directory) {
    return list(directory, __ -> true);
  }

  static List<Path> list(Path directory, Predicate<Path> filter) {
    try {
      return Files.list(directory).filter(filter).sorted().collect(Collectors.toList());
    } catch (IOException e) {
      throw new UncheckedIOException("list directory failed: " + directory, e);
    }
  }

  static Properties load(Properties properties, Path path) {
    if (Files.isRegularFile(path)) {
      try (var reader = Files.newBufferedReader(path)) {
        properties.load(reader);
      } catch (IOException e) {
        throw new UncheckedIOException("Reading properties failed: " + path, e);
      }
    }
    return properties;
  }

  /** Extract last path element from the supplied uri. */
  static Optional<String> findFileName(URI uri) {
    var path = uri.getPath();
    return path == null ? Optional.empty() : Optional.of(path.substring(path.lastIndexOf('/') + 1));
  }

  static Optional<String> findVersion(String jarFileName) {
    if (!jarFileName.endsWith(".jar")) return Optional.empty();
    var name = jarFileName.substring(0, jarFileName.length() - 4);
    var matcher = Pattern.compile("-(\\d+(\\.|$))").matcher(name);
    return (matcher.find()) ? Optional.of(name.substring(matcher.start() + 1)) : Optional.empty();
  }

  static <C extends Collection<?>> C requireNonEmpty(C collection, String name) {
    if (requireNonNull(collection, name + " must not be null").isEmpty()) {
      throw new IllegalArgumentException(name + " must not be empty");
    }
    return collection;
  }

  static <T> T requireNonNull(T object, String name) {
    return Objects.requireNonNull(object, name + " must not be null");
  }

  static <T> Optional<T> singleton(Collection<T> collection) {
    if (collection.isEmpty()) {
      return Optional.empty();
    }
    if (collection.size() != 1) {
      throw new IllegalStateException("Too many elements: " + collection);
    }
    return Optional.of(collection.iterator().next());
  }
  /** @see Files#createDirectories(Path, FileAttribute[])  */
  static Path treeCreate(Path path) {
    try {
      return Files.createDirectories(path);
    } catch (IOException e) {
      throw new UncheckedIOException("create directories failed: " + path, e);
    }
  }

  /** Delete all files and directories from and including the root directory. */
  static void treeDelete(Path root) {
    treeDelete(root, __ -> true);
  }

  /** Delete selected files and directories from and including the root directory. */
  static void treeDelete(Path root, Predicate<Path> filter) {
    if (filter.test(root)) { // trivial case: delete existing empty directory or single file
      try {
        Files.deleteIfExists(root);
        return;
      } catch (IOException ignored) {
        // fall-through
      }
    }
    try (var stream = Files.walk(root)) { // default case: walk the tree...
      var selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
      for (var path : selected.collect(Collectors.toList())) {
        Files.deleteIfExists(path);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("tree delete failed: " + root, e);
    }
  }
}
