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

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Nested
  class TestProject {
    @Test
    void alpha() {
      var base = Path.of("src/test-project/alpha");
      var bar = base.resolve("src/bar");
      var foo = base.resolve("src/foo");
      var paths = new Bach.Project.Paths(base);
      var expected =
          new Bach.Project.Builder()
              .base(base)
              .name("alpha")
              .version("0")
              .realm(
                  "main",
                  Set.of(Bach.Project.Realm.Modifier.DOCUMENT),
                  List.of(Path.of("src/{MODULE}/main/java")),
                  List.of(paths.lib()))
              .realm(
                  "test",
                  Set.of(Bach.Project.Realm.Modifier.TEST),
                  List.of(Path.of("src/{MODULE}/test/java"), Path.of("src/{MODULE}/test/module")),
                  List.of(paths.modules("main"), paths.lib()))
              .unit(bar.resolve("main/java"), bar, "main", List.of())
              .unit(foo.resolve("main/java"), foo, "main", List.of())
              .unit(foo.resolve("test/module"), foo, "test", List.of(Path.of("src/foo/main/java")))
              .build();
      var actual = Bach.Project.Builder.build(base);
      assertEquals(expected.base, actual.base);
      assertEquals(expected.name, actual.name);
      assertEquals(expected.version, actual.version);
      assertIterableEquals(expected.realms, actual.realms);
      assertIterableEquals(expected.units, actual.units);
      assertEquals(expected, actual);
    }
  }
}
