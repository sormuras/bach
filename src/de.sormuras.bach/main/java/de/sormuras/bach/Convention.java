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

import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IntSummaryStatistics;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  /** Return name of the main module by finding a single main class containing descriptor. */
  static Optional<String> mainModule(Stream<ModuleDescriptor> descriptors) {
    var mains = descriptors.filter(d -> d.mainClass().isPresent()).collect(Collectors.toList());
    return mains.size() == 1 ? Optional.of(mains.get(0).name()) : Optional.empty();
  }

  /** Return trailing integer part of {@code "java-{NUMBER}"} or zero. */
  static int javaReleaseFeatureNumber(String string) {
    if (string.startsWith("java-")) return Integer.parseInt(string.substring(5));
    return 0;
  }

  /** Return statistics summarizing over the passed {@code "java-{NUMBER}"} paths. */
  static IntSummaryStatistics javaReleaseStatistics(Stream<Path> paths) {
    var names = paths.map(Path::getFileName).map(Path::toString);
    return names.collect(Collectors.summarizingInt(Convention::javaReleaseFeatureNumber));
  }

  /** Extend the passed set of modules with missing JUnit test engine implementations. */
  static void amendJUnitTestEngines(Set<String> modules) {
    if (modules.contains("org.junit.jupiter") || modules.contains("org.junit.jupiter.api"))
      modules.add("org.junit.jupiter.engine");
    if (modules.contains("junit")) {
      modules.add("org.hamcrest");
      modules.add("org.junit.vintage.engine");
    }
  }

  /** Extend the passed set of modules with the JUnit Platform Console module. */
  static void amendJUnitPlatformConsole(Set<String> modules) {
    if (modules.contains("org.junit.platform.console")) return;
    var triggers =
        Set.of("org.junit.jupiter.engine", "org.junit.vintage.engine", "org.junit.platform.engine");
    modules.stream()
        .filter(triggers::contains)
        .findAny()
        .ifPresent(__ -> modules.add("org.junit.platform.console"));
  }
}
