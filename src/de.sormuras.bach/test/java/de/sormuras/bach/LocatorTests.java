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
import static org.junit.jupiter.api.Assertions.assertNull;

import de.sormuras.bach.internal.Locators;
import de.sormuras.bach.internal.Maven;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

class LocatorTests {
  @Nested
  class Jupiter {

    final String module = "org.junit.jupiter";
    final String uri = Maven.central("org.junit.jupiter", "junit-jupiter", "5.6.1");

    @Test
    void checkComposedLocator() {
      var locator = Project.Locator.of(Project.Locator.of(Map.of(module, uri)));
      assertEquals(uri, locator.apply(module));
      assertNull(locator.apply("foo"));
    }

    @Test
    void checkDirectLocator() {
      var locator = Project.Locator.of(Map.of(module, uri));
      assertEquals(uri, locator.apply(module));
      assertNull(locator.apply("foo"));
    }

    @Test
    @DisabledIfSystemProperty(named = "offline", matches = "true")
    void checkSormurasModulesLocator() {
      var locator = new Locators.SormurasModulesLocator(Map.of(module, "5.6.1"));
      assertEquals(uri, locator.apply(module));
      assertNull(locator.apply("foo"));
    }
  }
}
