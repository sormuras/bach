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

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ScannerTests {

  @Test
  void touchAllSetters() {
    new Scanner()
        .base("base", "more")
        .base(Path.of("base"))
        .base(Project.Base.of())
        .moduleInfoFiles(List.of())
        .offset("off", "set")
        .offset(Path.of(""))
        .limit(Integer.MAX_VALUE)
        .layout(Scanner.Layout.DEFAULT);
  }

  @Nested
  class DeSormurasBachTests {

    @Test
    void selfScan() {
      var scanner =
          new Scanner().offset(Path.of("src")).limit(5).layout(Scanner.Layout.MAIN_TEST_PREVIEW);
      var project = scanner.newBuilder().title("BACH").version("0-TEST").newProject();

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
    void scanJigsawQuickStart() {
      var base = Path.of("doc", "project", "JigsawQuickStart");
      var project = new Scanner().base(base).newBuilder().newProject();

      assertSame(base, project.base().directory());
      assertEquals(base.resolve(".bach/workspace"), project.base().workspace());
      assertEquals("JigsawQuickStart 1-ea", project.toTitleAndVersion());
      assertEquals(Set.of("com.greetings"), project.toDeclaredModuleNames());
      assertEquals(Set.of(), project.toRequiredModuleNames());
      var realm = project.realms().get(0);
      assertEquals("", realm.name());
    }

    @Test
    void scanJigsawQuickStartWorld() {
      var base = Path.of("doc", "project", "JigsawQuickStartWorld");
      var project = new Scanner().base(base).newBuilder().newProject();

      assertSame(base, project.base().directory());
      assertEquals(base.resolve(".bach/workspace"), project.base().workspace());
      assertEquals("JigsawQuickStartWorld 1-ea", project.toTitleAndVersion());
      assertEquals(Set.of("com.greetings", "org.astro", "test.modules"), project.toDeclaredModuleNames());
      assertEquals(Set.of("com.greetings", "org.astro"), project.toRequiredModuleNames());
      var realms = project.realms();
      assertEquals(2, realms.size(), realms.toString());
      var main = realms.get(0);
      assertEquals("main", main.name());
      assertEquals(2, main.units().size());
      var test = realms.get(1);
      assertEquals("test", test.name());
      assertEquals(2, test.units().size());
    }

    @Test
    void scanMultiRelease() {
      var base = Path.of("doc", "project", "MultiRelease");
      var project = new Scanner().base(base).newBuilder().newProject();

      var foo = project.realms().get(0).unit("com.foo").orElseThrow();
      assertFalse(foo.isMultiRelease());
      assertEquals(0, foo.sources().get(0).release());
      var bar = project.realms().get(0).unit("org.bar").orElseThrow();
      assertTrue(bar.isMultiRelease());
      assertEquals(8, bar.sources().get(0).release());
      assertEquals(9, bar.sources().get(1).release());
      assertEquals(11, bar.sources().get(2).release());
      var baz = project.realms().get(0).unit("org.baz").orElseThrow();
      assertTrue(baz.isMultiRelease());
      assertEquals(10, baz.sources().get(0).release());
    }

    @Test
    void scanSimplicissimus() {
      var base = Path.of("doc", "project", "Simplicissimus");
      var project = new Scanner().base(base).newBuilder().newProject();

      var foo = project.realms().get(0).unit("simplicius.simplicissimus").orElseThrow();
      assertFalse(foo.isMultiRelease());
      assertEquals(0, foo.sources().get(0).release());
    }
  }
}
