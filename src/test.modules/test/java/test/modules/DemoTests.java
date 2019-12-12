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

package test.modules;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    var log = new Log();
    var project = new ProjectBuilder(log).auto(Folder.of(Path.of("demo")));
    assertEquals("demo", project.name());
    assertEquals("0", project.version().toString());
    // main realm
    assertEquals("demo.core", project.unit("main", "demo.core").orElseThrow().name());
    assertEquals("demo.mantle", project.unit("main", "demo.mantle").orElseThrow().name());
    assertEquals("demo.shell", project.unit("main", "demo.shell").orElseThrow().name());
    // test realm
    assertEquals("demo.mantle", project.unit("test", "demo.mantle").orElseThrow().name());
    assertEquals("it", project.unit("test", "it").orElseThrow().name());
    // log.lines().forEach(System.out::println);

    var core = project.unit("main", "demo.core").orElseThrow();
    assertEquals(List.of(Path.of("demo/src/demo.core/main/resources")), core.resources());
  }

  @Test
  void build(@TempDir Path temp) throws Exception {
    var log = new Log();
    var base = Path.of("demo");
    var folder = new Folder(base, base.resolve("src"), temp.resolve("lib"), temp);
    var project = new ProjectBuilder(log).auto(folder);
    assertDoesNotThrow(
        () -> Bach.build(log, project),
        String.join("\n", log.lines()) + "\n" + String.join("\n", log.errors()));

    assertLinesMatch(
        List.of(
            ">> AUTO CONFIGURE DEMO PROJECT >>",
            "Bach.java (.+) initialized.",
            "Runtime information",
            ">> SYSTEM PROPERTIES >>",
            "Tools of the trade",
            ">> TOOLS >>",
            "Project demo 0",
            ">> REALMS, UNITS >>",
            "Executing task: BuildTask",
            "Executing task: SanityTask",
            "SanityTask took \\d+ millis.",
            "Executing task: ResolveTask",
            ">> MODULE SURVEYS, DOWNLOADS, ... >>",
            "Executing task: CompileTask",
            "Compiling 3 main unit(s): [demo.core, demo.shell, demo.mantle]",
            ">> JAVAC, JAR, ... >>",
            "Compiling 2 test unit(s): [demo.mantle, it]",
            ">> JAVAC, JAR, ... >>",
            "Executing task: TestTask",
            "Testing 2 test unit(s): [demo.mantle, it]",
            ">> TEST(MODULE), JUNIT(MODULE), ... >>",
            "Executing task: SummaryTask",
            "Tool runs",
            ">> TOOLS RUNS: javac, jar, javadoc, junit... >>",
            "Modules of main realm",
            "3 jar(s) found in: " + folder.modules("main").toUri(),
            ".+\\Q demo.core-0.jar\\E",
            ".+\\Q demo.mantle-0.jar\\E",
            ".+\\Q demo.shell-0.jar\\E",
            "Modules of test realm",
            "2 jar(s) found in: " + folder.modules("test").toUri(),
            ".+\\Q demo.mantle-0.jar\\E",
            ".+\\Q it-0.jar\\E",
            "Build \\d+ took millis.",
            "SummaryTask took \\d+ millis.",
            "BuildTask took \\d+ millis."),
        log.lines(),
        "Log lines don't match expectations:\n" + Files.readString(folder.out("summary.log")));

    assertEquals(
        0,
        log.getEntries().stream().filter(Log.Entry::isWarning).count(),
        "Expected zero warnings in log, but got:\n" + Files.readString(folder.out("summary.log")));

    var messages = log.getMessages();
    assertLinesMatch(
        messages.stream()
            .limit(messages.size() - 2) // "SummaryTask took..." and "BuildTask took..."
            .map(message -> ".+|.+|\\Q" + message + "\\E")
            .collect(Collectors.toList()),
        Files.readAllLines(folder.out("summary.log")));
  }
}
