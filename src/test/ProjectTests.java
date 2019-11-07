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
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProjectTests {
  @Test
  void generateSourceForBasicProject() {
    var project =
        new Bach.Project(Path.of(""), "foo", Version.parse("47.11"), List.of(), List.of());
    assertLinesMatch(
        List.of(
            "new Project(Path.of(\"\"), \"foo\", Version.parse(\"47.11\"), List.of(), List.of())"),
        project.toSourceLines());
  }

  @Test
  void generateSourceForProjectWithSingleModule() {
    var project =
        new Bach.Project(
            Path.of(""),
            "foo",
            Version.parse("47.11"),
            List.of(new Bach.Project.Realm("", List.of(Path.of("src")), List.of())),
            List.of(
                new Bach.Project.Unit(
                    ModuleDescriptor.newModule("foo").build(), Path.of("src/foo"), "", List.of())));
    assertLinesMatch(
        List.of(
            "new Project(",
            "    Path.of(\"\"),",
            "    \"foo\",",
            "    Version.parse(\"47.11\"),",
            "    List.of(",
            "        new Project.Realm(\"\", List.of(Path.of(\"src\")), List.of())",
            "    ),",
            "    List.of(",
            "        new Project.Unit(ModuleDescriptor.newModule(\"foo\").build(), Path.of(\"src/foo\"), \"\", List.of())",
            "    )",
            ")"),
        project.toSourceLines());
  }

  @Nested
  class TestProject {
    @Test
    void alpha() throws IOException {
      var base = Path.of("src/test-project/alpha");
      var bar = base.resolve("src/bar");
      var foo = base.resolve("src/foo");
      var paths = new Bach.Project.Paths(base);
      var expected =
          new Bach.Project.Builder()
              .base(base)
              .name("alpha")
              .version("0")
              .realm("main", List.of(Path.of("src/{MODULE}/main/java")), List.of(paths.lib()))
              .realm(
                  "test",
                  List.of(Path.of("src/{MODULE}/test/java"), Path.of("src/{MODULE}/test/module")),
                  List.of(paths.modules("main"), paths.lib()))
              .unit(
                  Bach.Modules.describe(
                      Files.readString(bar.resolve("main/java/module-info.java"))),
                  bar,
                  "main",
                  List.of())
              .unit(
                  Bach.Modules.describe(
                      Files.readString(foo.resolve("main/java/module-info.java"))),
                  foo,
                  "main",
                  List.of())
              .unit(
                  Bach.Modules.describe(
                      Files.readString(foo.resolve("test/module/module-info.java"))),
                  foo,
                  "test",
                  List.of(Path.of("src/foo/main/java")))
              .build();
      var actual = Bach.Project.Builder.build(base);
      assertEquals(expected.base, actual.base);
      assertEquals(expected.name, actual.name);
      assertEquals(expected.version, actual.version);
      assertIterableEquals(expected.realms, actual.realms);
      assertIterableEquals(expected.units, actual.units);
      assertEquals(expected, actual);
      assertLinesMatch(
          List.of(
              "new Project(",
              "    " + Bach.$(base) + ",",
              "    \"alpha\",",
              "    Version.parse(\"0\"),",
              "    List.of(",
              "        new Project.Realm(\"main\", List.of(Path.of(\"src/{MODULE}/main/java\")), List.of(Path.of(\"src/test-project/alpha/lib\"))),",
              "        new Project.Realm(\"test\", List.of(Path.of(\"src/{MODULE}/test/java\"), Path.of(\"src/{MODULE}/test/module\")), List.of(Path.of(\"src/test-project/alpha/.bach/out/main/modules\"), Path.of(\"src/test-project/alpha/lib\")))",
              "    ),",
              "    List.of(",
              "        new Project.Unit(ModuleDescriptor.newModule(\"bar\").build(), Path.of(\"src/test-project/alpha/src/bar\"), \"main\", List.of()),",
              "        new Project.Unit(ModuleDescriptor.newModule(\"foo\").build(), Path.of(\"src/test-project/alpha/src/foo\"), \"main\", List.of()),",
              "        new Project.Unit(ModuleDescriptor.newModule(\"foo\").build(), Path.of(\"src/test-project/alpha/src/foo\"), \"test\", List.of(Path.of(\"src/foo/main/java\")))",
              "    )",
              ")"),
          actual.toSourceLines());
    }
  }
}
