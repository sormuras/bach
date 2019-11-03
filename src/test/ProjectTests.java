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

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProjectTests {
  @Test
  void generateJavaSource() {
    var project = new Bach.Project("foo", Version.parse("47.11"), List.of());
    assertLinesMatch(
        List.of("new Project(\"foo\", Version.parse(\"47.11\"))"),
        new Bach.SourceGenerator().generate(project));
  }

  @Nested
  class TestProject {
    @Test
    void alpha() {
      var base = Path.of("src/test-project/alpha");
      var units = List.of(
          Bach.Project.Unit.of(base.resolve("src/bar/main/java/module-info.java")),
          Bach.Project.Unit.of(base.resolve("src/foo/main/java/module-info.java"))
      );
      var expected = new Bach.Project("alpha", Version.parse("0"), units);
      assertEquals(expected, Bach.Project.Builder.build(base));
    }
  }
}
