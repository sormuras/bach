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

package it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Task;
import de.sormuras.bach.project.Folder;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.Realm;
import de.sormuras.bach.project.Structure;
import de.sormuras.bach.project.Unit;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DemoTests {

  @Test
  void build(@TempDir Path temp) throws Exception {
    var folder = new Folder(Path.of("demo"), temp.resolve("lib"), temp);
    var main = new Realm("main");
    var core = new Unit(main, ModuleDescriptor.newModule("demo.core").build());
    var structure = new Structure(folder, List.of(main), List.of(core));
    var project = new Project("demo", Version.parse("1"), structure);

    var log = new Log();
    new Bach(log, project).execute(Task.build());

    assertLinesMatch(
        List.of("Modules folder not found: " + project.folder().modules("main")), log.errors());

    assertLinesMatch(
        List.of(
            "Bach.java " + Bach.VERSION + " initialized.",
            "Executing task: BuildTask",
            "Executing task: SummaryTask",
            "Modules of main realm",
            // WARNING "Modules folder not found: " + project.folder().modules("main"),
            "Build \\d+ took millis."),
        log.lines());

    assertEquals(1, log.getEntries().stream().filter(Log.Entry::isWarning).count());

    assertLinesMatch(
        log.getMessages().stream()
            .map(message -> ".+|.+|\\Q" + message + "\\E")
            .collect(Collectors.toList()),
        Files.readAllLines(temp.resolve("summary.log")));
  }
}
