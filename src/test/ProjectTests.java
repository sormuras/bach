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

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProjectTests {
  @Test
  void generateSourceForBasicProject() {
    var project = new Bach.Project(Path.of(""), "foo", Version.parse("47.11"), List.of());
    assertLinesMatch(
        List.of("new Project(Path.of(\"\"), \"foo\", Version.parse(\"47.11\"), List.of())"),
        project.toSourceLines());
  }

  @Test
  void generateSourceForProjectWithSingleModule() {
    var project =
        new Bach.Project(
            Path.of(""),
            "foo",
            Version.parse("47.11"),
            List.of(
                new Bach.Project.Unit(
                    Path.of("src/foo/module-info.java"),
                    ModuleDescriptor.newModule("foo").build(),
                    "src")));
    assertLinesMatch(
        List.of(
            "new Project(",
            "    Path.of(\"\"),",
            "    \"foo\",",
            "    Version.parse(\"47.11\"),",
            "    List.of(",
            "        Project.Unit.of(Path.of(\"src/foo/module-info.java\"))",
            "    )",
            ")"),
        project.toSourceLines());
  }

  @Nested
  class TestProject {
    @Test
    void alpha() {
      var base = Path.of("src/test-project/alpha");
      var bar = base.resolve("src/bar/main/java/module-info.java");
      var foo = base.resolve("src/foo/main/java/module-info.java");
      var expected =
          new Bach.Project.Builder()
              .base(base)
              .name("alpha")
              .version("0")
              .unit(bar)
              .unit(foo)
              .build();
      var actual = Bach.Project.Builder.build(base);
      assertEquals(expected, actual);
      assertLinesMatch(
          List.of(
              "new Project(",
              "    " + Bach.$(base) + ",",
              "    \"alpha\",",
              "    Version.parse(\"0\"),",
              "    List.of(",
              "        Project.Unit.of(" + Bach.$(bar) + "),",
              "        Project.Unit.of(" + Bach.$(foo) + ")",
              "    )",
              ")"),
          actual.toSourceLines());
    }
  }
}
