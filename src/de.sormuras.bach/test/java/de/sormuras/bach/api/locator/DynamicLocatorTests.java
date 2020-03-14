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

package de.sormuras.bach.api.locator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.api.Locator;
import de.sormuras.bach.api.Maven;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DynamicLocatorTests {

  final Locator locator = Locator.dynamicCentral(Map.of());

  @ParameterizedTest
  @ValueSource(strings = {"base", "controls", "fxml", "graphics", "media", "swing", "web"})
  void checkJavaFX(String id) {
    var module = "javafx." + id;
    var actual = locator.locate(module).orElseThrow().uri().toString();
    var expected = Maven.central("org.openjfx", "javafx-" + id, "14").toString();
    var regex = "\\Q" + expected.substring(0, expected.length() - 4) + "\\E-(win|mac|linux)\\.jar";
    assertTrue(actual.matches(regex), "\n" + actual + "\n" + regex);
  }
}
