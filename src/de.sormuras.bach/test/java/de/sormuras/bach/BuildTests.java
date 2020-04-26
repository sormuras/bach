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
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.util.Strings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import test.base.Tree;
import test.base.jdk.Classes;

// "Same Thread Execution" is required due to "jlink" doesn't work well
// in parallel, when started via its ToolProvider entry-point.
@Execution(ExecutionMode.SAME_THREAD)
class BuildTests {

  @Test
  void buildEmptyDirectoryFails(@TempDir Path temp) {
    var run = new Run(temp);
    var error = assertThrows(AssertionError.class, () -> run.bach().build(API.emptyProject()));
    assertEquals("Build project empty 0 (Task) failed", error.getMessage());
    assertEquals("project validation failed: no unit present", error.getCause().getMessage());
    assertLinesMatch(
        List.of(
            ">> BUILD >>",
            "java.lang.IllegalStateException: project validation failed: no unit present",
            ">> ERROR >>"),
        run.log().lines());
  }

  @Test
  void buildSingleton(@TempDir Path temp) throws Exception {
    var base = temp.resolve("singleton");
    var workspace = Workspace.of(base);
    var example = Projects.exampleOfSingleton(workspace);
    example.deploy(base);
    assertLinesMatch(List.of("module-info.java"), Tree.walk(base));

    var run = new Run(workspace.base());
    run.bach().build(example.project());

    run.log().assertThatEverythingIsFine();
    var N = Runtime.version().feature();
    assertLinesMatch(List.of(">> BUILD >>", "Build took .+"), run.log().lines());
    assertLinesMatch(
        List.of(
            ".bach",
            ">> PATHS >>",
            ".bach/workspace/classes/" + N + "/singleton/module-info.class",
            ">> PATHS >>",
            ".bach/workspace/summary.md",
            ">> PATHS >>"),
        Tree.walk(base),
        Strings.text(run.log().lines()));
  }

  @Test
  @DisabledIfSystemProperty(named = "offline", matches = "true")
  void buildSingletonWithRequires(@TempDir Path temp) throws Exception {
    var base = temp.resolve("singleton");
    var workspace = Workspace.of(base);
    var example = Projects.exampleOfSingleton(workspace, "jdk.jfr", "org.junit.jupiter");
    example.deploy(base);
    assertLinesMatch(List.of("module-info.java"), Tree.walk(base));

    var run = new Run(workspace.base());
    run.bach().build(example.project());

    run.log().assertThatEverythingIsFine();
    var N = Runtime.version().feature();
    assertLinesMatch(List.of(">> BUILD >>", "Build took .+"), run.log().lines());
    assertLinesMatch(
        List.of(
            ".bach",
            ">> PATHS >>",
            ".bach/workspace/classes/" + N + "/singleton/module-info.class",
            ">> PATHS >>",
            ".bach/workspace/summary.md",
            ">> PATHS >>"),
        Tree.walk(base),
        Strings.text(run.log().lines()));
  }

  @Test
  void buildJigsawQuickStartGreetings(@TempDir Path temp) throws Exception {
    var base = temp.resolve("greetings");
    var workspace = Workspace.of(base);
    var example = Projects.exampleOfJigsawQuickStartGreetings(workspace);
    example.deploy(base);
    assertLinesMatch(
        List.of(
            "src",
            ">> PATH>>",
            "src/com.greetings/com/greetings/Main.java",
            "src/com.greetings/module-info.java"),
        Tree.walk(base));

    var run = new Run(workspace.base());
    run.bach().build(example.project());

    run.log().assertThatEverythingIsFine();
    var N = Runtime.version().feature();
    assertLinesMatch(List.of(">> BUILD >>", "Build took .+"), run.log().lines());
    assertLinesMatch(
        List.of(
            ".bach",
            ">> PATHS >>",
            ".bach/workspace/classes/" + N + "/com.greetings/com/greetings/Main.class",
            ".bach/workspace/classes/" + N + "/com.greetings/module-info.class",
            ">> PATHS >>",
            ".bach/workspace/summary.md",
            ">> PATHS >>"),
        Tree.walk(base),
        Strings.text(run.log().lines()));
  }

  @Test
  void buildMultiRealmMultiModuleMultiRelease(@TempDir Path temp) throws Exception {
    var base = temp.resolve("MultiRealmMultiModuleMultiRelease");
    var workspace = Workspace.of(base);
    var example = Projects.exampleOfMultiRealmMultiModuleMultiRelease(workspace);
    example.deploy(base);

    var run = new Run(workspace.base());
    run.bach().build(example.project());

    // Files.readAllLines(workspace.workspace("summary.md")).forEach(System.out::println);

    run.log().assertThatEverythingIsFine();
    var N = Runtime.version().feature();
    assertLinesMatch(List.of(">> BUILD >>", "Build took .+"), run.log().lines());
    assertLinesMatch(
        List.of(
            ">> MODULAR API DOCUMENTATION >>",
            "classes/main/" + N + "/a/module-info.class",
            "classes/main/" + N + "/b/b/B.class",
            "classes/main/" + N + "/b/module-info.class",
            "classes/main/8/b/b/B.class",
            "classes/main/9/b/module-info.class",
            "modules/main/a@99.jar",
            "modules/main/b@99.jar",
            "summary.md",
            ">> MORE SUMMARIES >>"),
        Tree.walk(workspace.workspace(), Files::isRegularFile),
        Strings.text(run.log().lines()));
    assertEquals(8, Classes.feature(workspace.workspace("classes/main/8/b/b/B.class")));
  }
}
