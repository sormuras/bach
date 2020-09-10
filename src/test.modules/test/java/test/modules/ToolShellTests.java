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

import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ToolShellTests {

  @Test
  void checkTestShell() {
    var expectedToolNames = Stream.of("bach", "jar", "javac", ">>>>");
    var shell = new TestShell();
    assertEquals("javac", shell.javac().name());
    assertEquals("jdk.compiler", shell.javac().getClass().getModule().getName());

    var actualToolNames = shell.providers().map(ToolProvider::name).sorted();
    assertLinesMatch(expectedToolNames, actualToolNames);
  }
}
