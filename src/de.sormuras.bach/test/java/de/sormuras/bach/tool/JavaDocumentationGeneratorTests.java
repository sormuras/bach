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

package de.sormuras.bach.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.tool.JavaDocumentationGenerator.DestinationDirectory;
import de.sormuras.bach.tool.JavaDocumentationGenerator.DocumentListOfModules;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class JavaDocumentationGeneratorTests {

  @Test
  void canonical() {
    var modules = List.of("a", "b", "c");

    var javadoc =
        Tool.javadoc(
            List.of(new DocumentListOfModules(modules), new DestinationDirectory(Path.of("api"))));

    assertThrows(NoSuchElementException.class, () -> javadoc.get(Option.class));
    assertEquals(modules, javadoc.get(DocumentListOfModules.class).modules());
    assertEquals("api", javadoc.get(DestinationDirectory.class).value().toString());

    assertEquals("javadoc", javadoc.name());
    assertLinesMatch(
        List.of(
            "--module",
            "a,b,c",
            // "--module-version",
            // "123",
            // "--module-source-path",
            // String.join(File.separator, "src", "*", "main"),
            // "--module-path",
            // "lib",
            // "--patch-module",
            // "b=" + String.join(File.separator, "src", "b", "test"),
            // "-encoding",
            // "UTF-8",
            "-d",
            "api"),
        javadoc.toArgumentStrings());
  }
}
