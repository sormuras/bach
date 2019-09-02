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
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.Bach;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BachApiTests {

  @Test
  void instantiateViaNoArgsFactory() {
    var bach = assertDoesNotThrow(Bach::of);
    assertTrue(bach.toString().contains("Bach"));
  }

  @Test
  void version(@TempDir Path temp) {
    var bach = new Probe(temp);
    assertDoesNotThrow(bach::version);
    assertTrue(String.join("\n", bach.lines()).contains(Bach.VERSION));
  }

  @Test
  void help() {
    var bach = new Probe(Path.of(""));
    bach.help();
    assertLinesMatch(
        List.of(
            "F1! F1! F1!",
            "Method API",
            "  build (Bach)",
            "  clean (Bach)",
            "  compile (Bach)",
            "  help (Bach)",
            "  info (Bach)",
            "  resolve (Bach)",
            "  validate (Bach)",
            "  version (Bach)",
            "Provided tools",
            "  bach",
            ">> MORE FOUNDATION TOOLS >>"),
        bach.lines());
  }

  @Test
  void helpOnCustomBachDisplaysNewAndOverriddenMethods() {
    class Custom extends Probe {

      Custom() {
        super(Path.of(""));
      }

      @SuppressWarnings("unused")
      public void custom() {}

      @Override
      public void version() {}
    }

    var custom = new Custom();
    custom.help();
    assertLinesMatch(
        List.of(
            ">> PREFIX >>",
            "  build (Bach)",
            "  clean (Bach)",
            "  compile (Bach)",
            "  custom (Custom)",
            "  help (Bach)",
            "  info (Bach)",
            "  resolve (Bach)",
            "  validate (Bach)",
            "  version (Custom)",
            ">> SUFFIX >>"),
        custom.lines());
  }
}
