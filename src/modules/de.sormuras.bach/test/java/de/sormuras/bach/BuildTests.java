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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildTests {

  @Test
  void buildMinimalProject(@TempDir Path temp) throws Exception {
    var src = Files.createDirectories(temp.resolve("src"));
    var m = Files.createDirectories(src.resolve("m").resolve("main").resolve("java"));
    Files.write(m.resolve("module-info.java"), List.of("module m {}"));

    assertLinesMatch(List.of("src/m/main/java/module-info.java"), TestRun.treeWalk(temp));

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
            ">> TEST (NOOP) >>",
            "Build successful."),
        test.outLines());
    assertLinesMatch(
        List.of(
            "main/classes/m/module-info.class",
            ">> main/javadoc/**/ >>",
            "main/modules/m-1.0.0-SNAPSHOT.jar",
            "main/sources/m-1.0.0-SNAPSHOT-sources.jar"),
        TestRun.treeWalk(temp.resolve(bach.project.path(Project.Property.PATH_BIN))));
  }

  @Test
  void buildMinimalProjectWithTest(@TempDir Path temp) throws Exception {
    Files.write(
        temp.resolve(".properties"), List.of("path.lib=" + Bach.USER_PATH.resolve("lib").toUri()));
    var src = Files.createDirectories(temp.resolve("src"));
    var m = Files.createDirectories(src.resolve("m").resolve("main").resolve("java"));
    var t = Files.createDirectories(src.resolve("t").resolve("test").resolve("java"));
    Files.write(m.resolve("module-info.java"), List.of("module m {}"));
    Files.write(t.resolve("module-info.java"), List.of("open module t {}"));

    assertLinesMatch(
        List.of(
            ".properties", "src/m/main/java/module-info.java", "src/t/test/java/module-info.java"),
        TestRun.treeWalk(temp));

    var test = new TestRun(temp, temp).setOffline(true); // don't sync, using Bach's "lib" directory
    var bach = new Bach(test);
    var e = assertThrows(Error.class, bach::build, test::toString); // not test.toString()
    assertTrue(e.getMessage().startsWith("No tests found:"));
    assertLinesMatch(List.of("No tests found.+"), test.errLines());
    assertLinesMatch(
        List.of(
            ">> INIT >>",
            "Compiling main modules: [m]",
            ">> BUILD MAIN MODULES >>",
            "Jigsaw main compilation successful.",
            "Compiling test modules: [t]",
            ">> BUILD TEST MODULES >>",
            "Jigsaw test compilation successful.",
            ">> JUNIT >>",
            "[         0 tests successful      ]",
            "[         0 tests failed          ]",
            ""),
        test.outLines());
    assertLinesMatch(
        List.of(
            ".properties",
            "bin/main/classes/m/module-info.class",
            "bin/main/modules/m-1.0.0-SNAPSHOT.jar",
            "bin/main/sources/m-1.0.0-SNAPSHOT-sources.jar",
            "bin/test/classes/t/module-info.class",
            ">> TEST REPORTS >>",
            "bin/test/modules/t-1.0.0-SNAPSHOT.jar",
            "bin/test/sources/t-1.0.0-SNAPSHOT-sources.jar",
            "src/m/main/java/module-info.java",
            "src/t/test/java/module-info.java"),
        TestRun.treeWalk(temp));
  }
}
