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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.project.Structure;
import de.sormuras.bach.project.structure.Directory;
import de.sormuras.bach.project.structure.Location;
import de.sormuras.bach.project.structure.Realm;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectTests {
  @Test
  void empty() {
    var empty = API.emptyProject();
    assertEquals("empty", empty.name());
    assertEquals("0", empty.version().toString());
    assertEquals("", empty.structure().location().base().toString());
    assertEquals(0, empty.structure().realms().size());
    assertTrue(empty.toString().contains("empty"));
    assertEquals("empty 0", empty.toNameAndVersion());
    assertLinesMatch(
        List.of(
            "Project empty 0",
            "\tLocation",
            "\t\tbase='' -> .+/",
            "\t\tout=.bach[/\\\\]out",
            "\t\tlib=lib",
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
            new Structure(
                Location.of(),
                List.of(
                    new Realm(
                        "one",
                        1,
                        true,
                        List.of(API.newUnit("one", Directory.of(Path.of("one")))) //
                        ) //
                    )) //
            );
    assertLinesMatch(
        List.of(
            "Project one 1",
            "\tLocation",
            "\t\tbase='' -> .+/",
            "\t\tout=.bach[/\\\\]out",
            "\t\tlib=lib",
            "\tUnits: [one]",
            "\tRealms: [one]",
            "\t\tRealm \"one\"",
            "\t\t\trelease=1",
            "\t\t\tpreview=true",
            "\t\t\tUnits: [1]",
            "\t\t\t\tUnit \"one\"",
            "\t\t\t\t\tDirectories: [1]",
            "\t\t\t\t\t\tDirectory[path=one, type=UNDEFINED, release=0]"),
        one.toStrings());
  }
}
