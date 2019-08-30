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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/*BODY*/
/** Static helpers. */
/*STATIC*/ class Util {

  static Optional<Method> findApiMethod(Class<?> container, String name) {
    try {
      var method = container.getMethod(name);
      return isApiMethod(method) ? Optional.of(method) : Optional.empty();
    } catch (NoSuchMethodException e) {
      return Optional.empty();
    }
  }

  static boolean isApiMethod(Method method) {
    if (method.getDeclaringClass().equals(Object.class)) return false;
    if (Modifier.isStatic(method.getModifiers())) return false;
    return method.getParameterCount() == 0;
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
}
