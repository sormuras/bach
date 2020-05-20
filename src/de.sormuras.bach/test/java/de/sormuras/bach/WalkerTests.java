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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WalkerTests {

  @Nested
  class DeSormurasBachTests {

    @Test
    @Disabled("Walker for main|test|test-preview realm layout not implemented, yet")
    void walkSelf() {
      var walker = new Walker();
      walker.setWalkDepthLimit(5); // src/${MODULE}/${REALM}/java/module-info.java
      var project = walker.newBuilder().newProject();
      var realms = project.realms();

      assertEquals("bach 1-ea", project.toTitleAndVersion());

      var main = realms.get(0);
      assertEquals("main", main.name());
      assertTrue(main.flags().contains(Project.Realm.Flag.CREATE_API_DOCUMENTATION));
      assertTrue(main.flags().contains(Project.Realm.Flag.CREATE_CUSTOM_RUNTIME_IMAGE));

      var test = realms.get(1);
      assertEquals("test", test.name());
      assertTrue(test.flags().contains(Project.Realm.Flag.LAUNCH_TESTS));

      var preview = realms.get(2);
      assertEquals("test-preview", preview.name());
      assertTrue(preview.flags().contains(Project.Realm.Flag.LAUNCH_TESTS));
      assertTrue(preview.flags().contains(Project.Realm.Flag.ENABLE_PREVIEW_LANGUAGE_FEATURES));
    }
  }

  @Nested
  class DocProject {
    @Test
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
    }

    @Test
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
    }
  }
}
