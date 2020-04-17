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

package de.sormuras.bach.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LocatorTests {

  @Nested
  class Fragments {

    @Test
    void empty() {
      assertEquals("", Locator.toFragment(Map.of()));
      assertEquals(Map.of(), Locator.parseFragment(""));
    }

    @Test
    void fragmentsThatAreTooShortAreIllegal() {
      assertThrows(IllegalArgumentException.class, () -> Locator.parseFragment("1"));
      assertThrows(IllegalArgumentException.class, () -> Locator.parseFragment("II"));
    }

    @Test
    void fragmentsThatLackEqualsSignAreIllegal() {
      assertThrows(IllegalArgumentException.class, () -> Locator.parseFragment("a -> b"));
      assertThrows(IllegalArgumentException.class, () -> Locator.parseFragment("w/o:equals"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a=b", "size=123&name=123.jar"})
    void examples(String fragment) {
      var map = Locator.parseFragment(fragment);
      var actual = Locator.toFragment(map);
      assertEquals(fragment, actual);
    }
  }
}
