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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.internal.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WalkerTests {

  @Nested
  class IsMultiReleaseTests {

    @ParameterizedTest
    @ValueSource(
        strings = {
            "doc/project/JigsawQuickStart",
            "doc/project/JigsawQuickStartWorld",
            "doc/project/MultiRelease/com.foo",
            "src",
            "src/bach",
            "src/de.sormuras.bach",
            "src/de.sormuras.bach/main",
            "src/de.sormuras.bach/test",
            "src/test.base/test",
            "src/test.preview/test-preview"
        })
    void singleRelease(Path directory) {
      var directories = Paths.list(directory, Files::isDirectory);
      assertFalse(Walker.isMultiRelease(directories));
    }

    @ParameterizedTest
    @ValueSource(strings = {"doc/project/MultiRelease/org.bar", "doc/project/MultiRelease/org.baz"})
    void multiRelease(Path directory) {
      var directories = Paths.list(directory, Files::isDirectory);
      assertTrue(Walker.isMultiRelease(directories));
    }
  }

  @Nested
  class DeSormurasBachTests {

    @Test
    void walkSelf() {
      var walker =
          new Walker()
              .setWalkOffset(Path.of("src"))
              .setWalkDepthLimit(5)
              .setLayout(Walker.Layout.MAIN_TEST_PREVIEW);
      var project = walker.newBuilder().title("BACH").version("0-TEST").newProject();

      // project.toStrings().forEach(System.out::println);

      assertEquals("BACH 0-TEST", project.toTitleAndVersion());
      assertEquals(4, project.toUnits().count());

      var realms = project.realms();
      assertEquals(3, realms.size(), realms.toString());

      var main = realms.get(0);
      assertEquals("main", main.name());
      assertEquals(Set.of("de.sormuras.bach"), main.units().keySet());
      assertTrue(main.flags().contains(Project.Realm.Flag.CREATE_API_DOCUMENTATION));
      assertTrue(main.flags().contains(Project.Realm.Flag.CREATE_CUSTOM_RUNTIME_IMAGE));

      var test = realms.get(1);
      assertEquals("test", test.name());
      assertEquals("de.sormuras.bach", test.unit("de.sormuras.bach").orElseThrow().toName());
      assertEquals("test.base", test.unit("test.base").orElseThrow().toName());
      assertTrue(test.flags().contains(Project.Realm.Flag.LAUNCH_TESTS));

      var preview = realms.get(2);
      assertEquals("test-preview", preview.name());
      assertEquals(Set.of("test.preview"), preview.units().keySet());
      assertTrue(preview.flags().contains(Project.Realm.Flag.LAUNCH_TESTS));
      assertTrue(preview.flags().contains(Project.Realm.Flag.ENABLE_PREVIEW_LANGUAGE_FEATURES));
    }
  }

  @Nested
  class DocProject {
    @Test
    @ResourceLock("jlink")
    void walkJigsawQuickStart() {
      var base = Path.of("doc", "project", "JigsawQuickStart");
      var project = new Walker().setBase(base).newBuilder().newProject();
      assertSame(base, project.base().directory());
      assertEquals(base.resolve(".bach/workspace"), project.base().workspace());
      assertEquals("JigsawQuickStart 1-ea", project.toTitleAndVersion());
      assertEquals(Set.of("com.greetings"), project.toDeclaredModuleNames());
      assertEquals(Set.of(), project.toRequiredModuleNames());
      var realm = project.realms().get(0);
      assertEquals("", realm.name());

      Bach.of(project).build().assertSuccessful();
    }

    @Test
    @ResourceLock("jlink")
    void walkJigsawQuickStartWorld() {
      var base = Path.of("doc", "project", "JigsawQuickStartWorld");
      var project = new Walker().setBase(base).newBuilder().newProject();
      assertSame(base, project.base().directory());
      assertEquals(base.resolve(".bach/workspace"), project.base().workspace());
      assertEquals("JigsawQuickStartWorld 1-ea", project.toTitleAndVersion());
      assertEquals(
          Set.of("com.greetings", "org.astro", "test.modules"), project.toDeclaredModuleNames());
      assertEquals(Set.of("com.greetings", "org.astro"), project.toRequiredModuleNames());
      var realms = project.realms();

      Assumptions.assumeTrue(realms.size() == 2);

      assertEquals(2, realms.size(), realms.toString());
      var main = realms.get(0);
      assertEquals("main", main.name());
      assertEquals(2, main.units().size());
      var test = realms.get(1);
      assertEquals("test", test.name());
      assertEquals(2, test.units().size());

      Bach.of(project).build().assertSuccessful();
    }

    @Test
    @ResourceLock("jlink")
    void walkMultiRelease() {
      var base = Path.of("doc", "project", "MultiRelease");
      var project = new Walker().setBase(base).newBuilder().newProject();

      Bach.of(project).build().assertSuccessful();
    }
  }
}
