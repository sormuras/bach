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
import de.sormuras.bach.project.ProjectBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DemoTests {

  @Test
  void autoConfigureDemoProjectAndCheckComponents() {
    var project = ProjectBuilder.build(Path.of("demo"));
    assertEquals("demo", project.name());
    assertEquals("0", project.version().toString());
    assertEquals("demo.core", project.unit("main", "demo.core").orElseThrow().name());
  }

  @Test
  void build(@TempDir Path temp) throws Exception {
    var log = new Log();
    var folder = new Folder(Path.of("demo"), temp.resolve("lib"), temp);
    new Bach(log, ProjectBuilder.build(folder)).execute(Task.build());

    assertLinesMatch(
        List.of(
            "Bach.java " + Bach.VERSION + " initialized.",
            "Executing task: BuildTask",
            "Executing task: SanityTask",
            "Executing task: ResolveTask",
            "TODO Resolving missing external modules",
            "Executing task: CompileTask",
            "Compiling 2 main unit(s): [demo.core, demo.mantle]",
            ">> JAVAC, JAR, ... >>",
            "Compiling 2 test unit(s): [demo.mantle, it]",
            ">> JAVAC, JAR, ... >>",
            "Executing task: TestTask",
            "TODO Testing modules",
            "Executing task: SummaryTask",
            "Modules of main realm",
            "2 jar(s) found in: " + folder.modules("main").toUri(),
            String.format("%,11d demo.core-0.jar", 1600),
            String.format("%,11d demo.mantle-0.jar", 1175),
            "Modules of test realm",
            "2 jar(s) found in: " + folder.modules("test").toUri(),
            String.format("%,11d demo.mantle-0.jar", 2157),
            String.format("%,11d it-0.jar", 612),
            "Build \\d+ took millis."),
        log.lines());

    assertEquals(0, log.getEntries().stream().filter(Log.Entry::isWarning).count());

    assertLinesMatch(
        log.getMessages().stream()
            .map(message -> ".+|.+|\\Q" + message + "\\E")
            .collect(Collectors.toList()),
        Files.readAllLines(folder.out("summary.log")));
  }
}
