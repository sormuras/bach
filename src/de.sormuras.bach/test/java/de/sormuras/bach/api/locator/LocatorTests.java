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

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.sormuras.bach.api.Maven;
import de.sormuras.bach.internal.Resources;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import test.base.Log;

class LocatorTests {

  final HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();

  @Nested
  class Jupiter {

    final String module = "org.junit.jupiter";
    final URL url = Maven.central("org.junit.jupiter", "junit-jupiter", "5.6.0");

    @Test
    void checkDirectLocator() {
      var locator = new DirectLocator(Map.of(module, url));
      assertEquals(url, locator.locate(module).orElseThrow());
    }

    @Test
    void checkDynamicLocator() {
      var locator = new DynamicLocator(Maven.CENTRAL_REPOSITORY, Map.of());
      assertEquals(url, locator.locate(module).orElseThrow());
    }

    @Test
    @DisabledIfSystemProperty(named = "offline", matches = "true")
    void checkSormurasModulesLocator() {
      var log = new Log();
      var resources = new Resources(log, client);
      var locator = new SormurasModulesLocator(Map.of(), resources);
      assertEquals(url, locator.locate(module).orElseThrow());
      log.assertThatEverythingIsFine();
    }
  }
}
