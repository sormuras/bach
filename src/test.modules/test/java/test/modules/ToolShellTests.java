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
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import de.sormuras.bach.ToolShell;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ToolShellTests {

  @Test
  void providers() {
    var expectedToolNames = List.of("bach", "jar", "javac", ">>>>");
    var shell =
        new ToolShell() {
          ToolProvider javac() {
            return computeToolProvider("javac", "java.base", List.of());
          }

          Stream<ToolProvider> providers(String module) {
            return computeToolProviders(module, List.of());
          }
        };
    assertEquals("javac", shell.javac().name());
    assertEquals("jdk.compiler", shell.javac().getClass().getModule().getName());

    var module = "java.base";
    var actualToolNames = shell.providers(module).map(ToolProvider::name).sorted();
    assertLinesMatch(expectedToolNames, actualToolNames.collect(Collectors.toList()));
  }
}
