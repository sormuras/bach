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

package test.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Project;
import de.sormuras.bach.project.Information;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.project.Structure;
import java.lang.module.ModuleDescriptor;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class BachTests {

  @Test
  void checkStringRepresentationIsLegit() {
    assertEquals("Bach.java " + Bach.VERSION, new Bach().toString());
  }

  @Test
  void text() {
    var actualLines = new Project(
            "Text Blocks",
             ModuleDescriptor.Version.parse("14-preview"),
             Information.of(),
             new Structure(List.of(), null, Library.of()))
        .toStrings();

    assertLinesMatch("""
        Project
        	name="Text Blocks"
        	version=14-preview
        Information
        	description=""
        	uri=null
        Structure
        	Units: []
        	Realms: []
        """.lines().collect(Collectors.toList()),
        actualLines);
  }
}
