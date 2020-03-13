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

package de.sormuras.bach;

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Common conventions.
 *
 * @see <a href="https://github.com/sormuras/bach#common-conventions">Common Conventions</a>
 */
public interface Convention {

  /** Return name of main class of the specified module. */
  static Optional<String> mainClass(Path info, String module) {
    var main = Path.of(module.replace('.', '/'), "Main.java");
    var exists = Files.isRegularFile(info.resolveSibling(main));
    return exists ? Optional.of(module + '.' + "Main") : Optional.empty();
  }

  /** Extend the passed map of modules with missing JUnit test engine implementations. */
  static void amendJUnitTestEngines(Map<String, Version> modules) {
    var names = modules.keySet();
    if (names.contains("org.junit.jupiter") || names.contains("org.junit.jupiter.api"))
      modules.putIfAbsent("org.junit.jupiter.engine", null);
    if (names.contains("junit")) modules.putIfAbsent("org.junit.vintage.engine", null);
  }

  /** Extend the passed map of modules with the JUnit Platform Console module. */
  static void amendJUnitPlatformConsole(Map<String, Version> modules) {
    var names = modules.keySet();
    if (names.contains("org.junit.platform.console")) return;
    var triggers =
        Set.of("org.junit.jupiter.engine", "org.junit.vintage.engine", "org.junit.platform.engine");
    names.stream()
        .filter(triggers::contains)
        .findAny()
        .ifPresent(__ -> modules.put("org.junit.platform.console", null));
  }
}
