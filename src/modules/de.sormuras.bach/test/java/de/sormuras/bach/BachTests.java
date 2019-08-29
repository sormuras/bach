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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleDescriptor;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BachTests (de.sormuras.bach)")
class BachTests {
  @Test
  void instantiate() {
    assertNotNull(Bach.of().toString());
  }

  @Test
  void banner() {
    assertFalse(Bach.of().getBanner().isBlank());
  }

  @Test
  void help() {
    var out = new StringWriter();
    var bach = new Bach(new PrintWriter(out), new PrintWriter(System.err));
    bach.help();
    assertLinesMatch(
        List.of(
            "F1! F1! F1!",
            "Method API",
            "  help (Bach)",
            "  version (Bach)",
            "Provided tools",
            "  jar",
            ">> MORE FOUNDATION TOOLS >>",
            "  jmod"),
        out.toString().lines().collect(Collectors.toList()));
  }

  @Test
  void helpOnCustomBachDisplaysNewAndOverriddenMethods() {
    class CustomBach extends Bach {
      CustomBach(StringWriter out) {
        super(new PrintWriter(out), new PrintWriter(System.err));
      }
      public void custom() {}
      @Override
      public void version() {}
    }
    var actual = new StringWriter();
    var custom = new CustomBach(actual);
    custom.help();
    assertLinesMatch(
        List.of(
            ">> PREFIX >>",
            "  custom (CustomBach)",
            "  help (Bach)",
            "  version (CustomBach)",
            ">> SUFFIX >>"),
        actual.toString().lines().collect(Collectors.toList()));
  }

  @Test
  void version() {
    assertDoesNotThrow(() -> ModuleDescriptor.Version.parse(Bach.VERSION));
  }
}
