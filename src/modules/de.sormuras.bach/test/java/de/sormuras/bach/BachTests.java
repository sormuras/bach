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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.module.ModuleDescriptor;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class BachTests {
  @Test
  void banner() {
    assertFalse(Bach.of().getBanner().isBlank());
  }

  @Test
  void checkDefaultValues() {
    var bach = Bach.of();
    assertNotNull(bach.out);
    assertNotNull(bach.err);
    assertEquals(Path.of(""), bach.configuration.getHomeDirectory());
    assertEquals(Path.of("bin"), bach.configuration.getWorkspaceDirectory());
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
      private Custom() {
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
            "  custom (Custom)",
            "  help (Bach)",
            "  info (Bach)",
            "  resolve (Bach)",
            "  validate (Bach)",
            "  version (Custom)",
            ">> SUFFIX >>"),
        custom.lines());
  }

  @Test
  void validateWorksInDefaultFileSystemRootDirectories() {
    for (var path : FileSystems.getDefault().getRootDirectories()) {
      if (Files.isDirectory(path)) {
        var bach = new Probe(path);
        assertDoesNotThrow(bach::validate);
      }
    }
  }

  @Test
  void versionIsLegalByModuleDescriptorVersionsParseFactoryContract() {
    assertDoesNotThrow(() -> ModuleDescriptor.Version.parse(Bach.VERSION));
  }
}
