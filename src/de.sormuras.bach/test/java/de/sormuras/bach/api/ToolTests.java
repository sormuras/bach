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

package de.sormuras.bach.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolTests {

  @Test
  void javac() {
    var javac = new Tool.JavaCompiler();
    assertEquals("javac", javac.name());
    assertEquals("javac", javac.toString());
    javac.setVerbose(true);
    javac.setVersion(true);
    javac.setDestinationDirectory(Path.of("classes"));
    javac.setGenerateMetadataForMethodParameters(true);
    javac.setTerminateCompilationIfWarningsOccur(true);
    assertLinesMatch(
        List.of("-d", "classes", "-parameters", "-Werror", "-verbose", "-version"), javac.args());
    assertTrue(javac.toString().startsWith("javac "));
    assertTrue(javac.toString().endsWith("-version"));
  }
}
