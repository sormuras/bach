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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

class LocatorTests {

  private final String CENTRAL = "https://repo.maven.apache.org/maven2";

  @Nested
  class Jupiter {

    final String module = "org.junit.jupiter";

    final String group = "org.junit.jupiter";
    final String artifact = "junit-jupiter";
    final String version = "5.6.1";

    final String coordinates = String.join(":", group, artifact, version);
    final String path = group.replace('.', '/');
    final String file = artifact + "-" + version + ".jar";
    final String expected = String.join("/", CENTRAL, path, artifact, version, file);

    @Test
    void checkComposedLocator() {
      var locator = Project.Locator.of(Project.Locator.of(Map.of(module, expected)));
      assertEquals(expected, locator.locate(module).orElseThrow());
      assertTrue(locator.locate("foo").isEmpty());
    }

    @Test
    void checkDirectLocator() {
      var locator = Project.Locator.of(Map.of(module, expected));
      assertEquals(expected, locator.locate(module).orElseThrow());
      assertTrue(locator.locate("foo").isEmpty());
    }

    @Test
    void checkMavenCentralLocator() {
      var locator = Project.Locator.ofMaven(Map.of(module, coordinates));
      assertEquals(expected, locator.locate(module).orElseThrow());
      assertTrue(locator.locate("foo").isEmpty());
    }

    @Test
    void checkMavenRepositoryLocator() {
      var locator = Project.Locator.ofMaven(CENTRAL, Map.of(module, coordinates));
      assertEquals(expected, locator.locate(module).orElseThrow());
      assertTrue(locator.locate("foo").isEmpty());
    }

    @Test
    void checkMavenLocatorWithDirectMapping() {
      var locator = Project.Locator.ofMaven(CENTRAL, Map.of(module, expected));
      assertEquals(expected, locator.locate(module).orElseThrow());
      assertTrue(locator.locate("foo").isEmpty());
    }

    @Test
    @DisabledIfSystemProperty(named = "offline", matches = "true")
    void checkSormurasModulesLocator() {
      var locator = Project.Locator.ofSormurasModules(Map.of(module, version));
      locator.accept(Bach.of(Projects.zero()));
      assertEquals(expected, locator.locate(module).orElseThrow());
      assertTrue(locator.locate("foo").isEmpty());
    }
  }
}
