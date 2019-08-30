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

package it;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.Bach;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ProjectTests {

  private static final Path PROJECTS = Path.of("src/test-project");

  private static Stream<Path> projects() throws Exception {
    try (var entries = Files.list(PROJECTS)) {
      return entries.filter(Files::isDirectory).sorted().collect(Collectors.toList()).stream();
    }
  }

  @ParameterizedTest
  @MethodSource("projects")
  void help(Path home) {
    var bach = new Probe(home);
    assertDoesNotThrow(bach::help, "bach::help failed: " + home);
    assertLinesMatch(
        List.of(
            "F1! F1! F1!",
            "Method API",
            ">> METHODS AND DECLARING CLASSES >>",
            "Provided tools",
            ">> NAMES OF TOOLS >>"),
        bach.lines());
  }

  @ParameterizedTest
  @MethodSource("projects")
  void info(Path home) {
    var bach = new Probe(home);
    assertDoesNotThrow(bach::info, "bach::info failed: " + home);
    assertLinesMatch(
        List.of(
            "Bach \\(" + Bach.VERSION + ".*\\)",
            "  home = '" + home + "' -> " + home.toUri(),
            "  workspace = '" + home.resolve("bin") + "'",
            "  library paths = [" + home.resolve("lib") + "]",
            "  source directories = [" + home.resolve("src") + "]"
        ),
        bach.lines());
  }

  @Nested
  class Empty {

    @Test
    void build(@TempDir Path work) {
      var home = PROJECTS.resolve("empty");
      var e = assertThrows(Error.class, () -> new Probe(home, work).build());
      assertEquals("expected that home contains a directory: " + home.toUri(), e.getMessage());
    }
  }

  @Nested
  class MissingModule {

    @Test
    void build(@TempDir Path work) {
      var probe = new Probe(PROJECTS.resolve("missing-module"), work);
      assertDoesNotThrow(probe::build);
      assertLinesMatch(List.of(">> BUILD >>"), probe.lines());
    }
  }

  @Nested
  class RequiresAsm {

    @Test
    void build(@TempDir Path work) {
      var probe = new Probe(PROJECTS.resolve("requires-asm"), work);
      assertDoesNotThrow(probe::build);
      assertLinesMatch(List.of(">> BUILD >>"), probe.lines());
    }
  }
}
