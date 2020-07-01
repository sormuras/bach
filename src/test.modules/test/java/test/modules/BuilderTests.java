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

package test.modules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Builder;
import java.io.File;
import java.lang.module.FindException;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BuilderTests {

  @Nested
  class ModuleSourcePathTests {

    private final Builder builder = new Builder(Bach.ofSystem());

    @Test
    void modulePatternFormFromPathWithoutModulesNameFails() {
      var path = Path.of("a/b/c/module-info.java");
      var exception =
          assertThrows(FindException.class, () -> builder.toModuleSourcePathPatternForm(path, "d"));
      assertEquals("Name 'd' not found: " + path, exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
      ".               , foo/module-info.java",
      "src             , src/foo/module-info.java",
      "./*/src         , foo/src/module-info.java",
      "src/*/main/java , src/foo/main/java/module-info.java"
    })
    void modulePatternFormForModuleFoo(String expected, Path path) {
      var actual = builder.toModuleSourcePathPatternForm(path, "foo");
      assertEquals(expected.replace('/', File.separatorChar), actual);
    }
  }
}
