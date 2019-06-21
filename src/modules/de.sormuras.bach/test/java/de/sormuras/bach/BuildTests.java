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

package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildTests {

  @Test
  void buildMinimalProject(@TempDir Path temp) throws Exception {
    var src = Files.createDirectories(temp.resolve("src"));
    var m = Files.createDirectories(src.resolve("m").resolve("main").resolve("java"));
    Files.write(m.resolve("module-info.java"), List.of("module m {}"));

    assertLinesMatch(
        List.of(
            "src",
            ">>>>",
            "src/m/main/java/module-info.java"),
        TestRun.treeWalk(temp));

    var test = new TestRun(temp, temp);
    var bach = new Bach(test);
    assertDoesNotThrow(bach::build, test::toString); // not test.toString()

    assertLinesMatch(List.of(), test.errLines());
    assertLinesMatch(
        List.of(
            ">> INIT >>",
            "Compiling main modules: [m]",
            ">> BUILD MAIN MODULES >>",
            "No test modules found.",
            "Build successful."),
        test.outLines());
    assertLinesMatch(
        List.of(
            ">>>>",
            "main/classes/m/module-info.class",
            "main/modules",
            "main/modules/m-1.0.0-SNAPSHOT.jar"),
        TestRun.treeWalk(temp.resolve(bach.project.path(Project.Property.PATH_BIN))));
  }

  @Test
  @Disabled
  void buildMinimalProjectWithTest(@TempDir Path temp) throws Exception {
    var src = Files.createDirectories(temp.resolve("src"));
    var m = Files.createDirectories(src.resolve("m").resolve("main").resolve("java"));
    var t = Files.createDirectories(src.resolve("t").resolve("test").resolve("java"));
    Files.write(m.resolve("module-info.java"), List.of("module m {}"));
    Files.write(t.resolve("module-info.java"), List.of("open module t {}"));

    assertLinesMatch(
        List.of(
            "src",
            ">>>>",
            "src/m/main/java/module-info.java",
            ">>>>",
            "src/t/test/java/module-info.java"),
        TestRun.treeWalk(temp));

    var test = new TestRun(temp, temp);
    var bach = new Bach(test);
    assertDoesNotThrow(bach::build, test::toString); // not test.toString()

    assertLinesMatch(List.of(), test.errLines());
    assertLinesMatch(
        List.of(
            ">> INIT >>",
            "Compiling main modules: [m]",
            ">> BUILD MAIN MODULES >>",
            "Compiling test modules: [t]",
            ">> BUILD TEST MODULES >>",
            "Build successful.",
            ">> JUNIT >>",
            "[         4 tests successful      ]",
            "[         0 tests failed          ]",
            "",
            "JUnit successful."),
        test.outLines());
    assertLinesMatch(
        List.of(
            "target",
            ">>>>",
            "target/bach/main/classes/de.sormuras.bach/de/sormuras/bach/Bach.class",
            ">>>>",
            "target/bach/main/classes/de.sormuras.bach/module-info.class",
            ">>>>",
            "target/bach/main/modules/de.sormuras.bach-" + Bach.VERSION + ".jar",
            ">>>>",
            "target/bach/test/modules/integration-" + Bach.VERSION + ".jar"),
        TestRun.treeWalk(temp));
  }
}
