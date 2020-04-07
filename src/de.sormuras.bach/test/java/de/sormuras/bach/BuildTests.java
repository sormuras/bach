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

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.base.Log;
import test.base.Tree;

class BuildTests {

  @Test
  void buildJigsawQuickStartGreetings(@TempDir Path temp) throws Exception {
    var example = Projects.exampleOfJigsawQuickStartGreetings();
    var base = example.deploy(temp.resolve("greetings"));
    assertLinesMatch(
        List.of(
            "src",
            ">> PATH>>",
            "src/com.greetings/com/greetings/Main.java",
            "src/com.greetings/module-info.java"),
        Tree.walk(base));

    var log = new Log();
    var bach = new Bach(new Printer.Default(log, Level.ALL), Workspace.of(base));
    bach.build(example.project());

    log.assertThatEverythingIsFine();
    assertLinesMatch(List.of(">> BUILD >>", "Build took .+"), log.lines());
    assertLinesMatch(
        List.of(".bach", ">> PATHS >>", ".bach/workspace/summary.md", ">> PATHS >>"),
        Tree.walk(base));
  }
}
