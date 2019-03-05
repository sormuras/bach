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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DemoTests {

  @Nested
  @DisplayName("jigsaw-quick-start")
  class JigsawQuickStart {

    @ParameterizedTest
    @ValueSource(strings = {"greetings", "greetings-world"})
    void greetings(String name, @TempDir Path workspace) throws Exception {
      var demo = Path.of("demo", "jigsaw-quick-start", name);
      var base = workspace.resolve(demo.getFileName());
      Util.treeCopy(demo, base);

      var out = new ArrayList<String>();
      var bach = new Bach(true, base);
      bach.log.out = out::add;
      var resources = Path.of("src", "test-resources");
      assertEquals(base, bach.base);
      assertEquals(name, bach.project.name);
      // assertEquals("1.0.0-SNAPSHOT", bach.project.version);
      //  assertEquals(
      //    "com.greetings/com.greetings.Main",
      //    Bach.ModuleInfo.findLaunch(base.resolve("src")).orElseThrow());

      var cleanTreeWalk = resources.resolve(demo.resolveSibling(name + ".clean.txt"));
      assertLinesMatch(Files.readAllLines(cleanTreeWalk), Util.treeWalk(base));

      bach.build();
      assertLinesMatch(
          List.of("main.compile()", ">> MAIN >>", "test.compile()", ">> TEST >>"), out);

      var buildTreeWalk = resources.resolve(demo.resolveSibling(name + ".build.txt"));
      assertLinesMatch(Files.readAllLines(buildTreeWalk), Util.treeWalk(base));

      // bach.run(Bach.Action.Default.ERASE);
      // assertLinesMatch(Files.readAllLines(cleanTreeWalk), bach.utilities.treeWalk(base));
    }
  }
}
