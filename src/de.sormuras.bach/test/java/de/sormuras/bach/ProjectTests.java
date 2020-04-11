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

package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.project.Information;
import de.sormuras.bach.project.Structure;
import de.sormuras.bach.project.Directory;
import de.sormuras.bach.project.Realm;
import de.sormuras.bach.tool.JavaCompiler;
import de.sormuras.bach.tool.Tool;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectTests {
  @Test
  void empty() {
    var empty = API.emptyProject();
    assertEquals("empty", empty.name());
    assertEquals("0", empty.version().toString());
    assertTrue(empty.information().description().isEmpty());
    assertNull(empty.information().uri());
    assertEquals(0, empty.structure().realms().size());
    assertTrue(empty.toString().contains("empty"));
    assertEquals("empty 0", empty.toNameAndVersion());
    assertLinesMatch(
        List.of(
            "Project",
            "\tname=\"empty\"",
            "\tversion=0",
            "Information",
            "\tdescription=\"\"",
            "\turi=null",
            "Structure",
            "\tUnits: []",
            "\tRealms: []"),
        empty.toStrings());
  }

  @Test
  void oneOfEach() {
    var one =
        new Project(
            "one",
            Version.parse("1"),
            new Information("one", URI.create("one")),
            new Structure(
                List.of(
                    new Realm(
                        "one",
                        List.of(API.newUnit("one", Directory.of(Path.of("one")))), //
                        null,
                        Tool.javac(
                            List.of(
                                new JavaCompiler.CompileForJavaRelease(1),
                                new JavaCompiler.EnablePreviewLanguageFeatures()))) //
                    ), //
                "one") //
            );
    assertLinesMatch(
        List.of(
            "Project",
            "\tname=\"one\"",
            "\tversion=1",
            "Information",
            "\tdescription=\"one\"",
            "\turi=one",
            "Structure",
            "\tUnits: [one]",
            "\tRealms: [one]",
            "\tmain-realm=\"one\"",
            "\tRealm \"one\"",
            "\t\trelease=1",
            "\t\tpreview=true",
            "\t\tUnits: [1]",
            "\t\tUnit \"one\"",
            "\t\t\tDirectories: [1]",
            "\t\t\tDirectory[path=one, type=UNKNOWN, release=0]"),
        one.toStrings());
  }
}
