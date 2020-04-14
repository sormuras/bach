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

import java.net.URI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LocatorTests {

  @Nested
  class Locations {

    @Test
    void empty() {
      var empty = new Locator.Location(URI.create("location:empty"), null);
      assertEquals("location", empty.uri().getScheme());
      assertEquals("empty", empty.uri().getSchemeSpecificPart());
      assertEquals("", empty.toVersionString());
    }

    @Test
    void one() {
      var empty = new Locator.Location(URI.create("location:one@1"), "1");
      assertEquals("location", empty.uri().getScheme());
      assertEquals("one@1", empty.uri().getSchemeSpecificPart());
      assertEquals("-1", empty.toVersionString());
    }
  }
}
