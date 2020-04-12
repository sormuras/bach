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

import de.sormuras.bach.tool.JavaArchiveTool.ArchiveFile;
import de.sormuras.bach.tool.JavaArchiveTool.ChangeDirectory;
import de.sormuras.bach.tool.JavaArchiveTool.MultiReleaseVersion;
import de.sormuras.bach.tool.JavaArchiveTool.Operation;
import de.sormuras.bach.tool.JavaArchiveTool.PerformOperation;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class JavaArchiveToolTests {

  @Test
  void canonical() {
    var jar =
        Tool.jar(
            List.of(
                new PerformOperation(Operation.CREATE),
                new PerformOperation(Operation.GENERATE_INDEX, "file"),
                new PerformOperation(Operation.LIST),
                new PerformOperation(Operation.UPDATE),
                new PerformOperation(Operation.EXTRACT),
                new PerformOperation(Operation.DESCRIBE_MODULE),
                new ArchiveFile(Path.of("foo-123.jar")),
                new MultiReleaseVersion(321),
                new ChangeDirectory(Path.of("classes"))));

    assertThrows(NoSuchElementException.class, () -> jar.get(Option.class));
    assertEquals(6, jar.filter(PerformOperation.class).count());
    assertEquals("foo-123.jar", jar.get(ArchiveFile.class).value().toString());
    assertEquals(321, jar.get(MultiReleaseVersion.class).version());
    assertEquals("classes", jar.get(ChangeDirectory.class).value().toString());

    assertEquals("jar", jar.name());
    assertLinesMatch(
        List.of(
            "--create",
            "--generate-index=file",
            "--list",
            "--update",
            "--extract",
            "--describe-module",
            "--file",
            "foo-123.jar",
            "--release",
            "321",
            "-C",
            "classes",
            "."),
        jar.toArgumentStrings());
  }

  @Test
  void allAdditionalArgumentsArePreserved() {
    var operation = new PerformOperation(Operation.GENERATE_INDEX, "first", "second", "third");
    assertEquals(Operation.GENERATE_INDEX, operation.mode());
    assertEquals(List.of("first", "second", "third"), operation.more());
  }
}
