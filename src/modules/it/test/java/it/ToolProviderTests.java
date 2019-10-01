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

package it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class ToolProviderTests {

  private Optional<ToolProvider> findBach() {
    var currentClassLoader = getClass().getClassLoader();
    if (currentClassLoader == ClassLoader.getSystemClassLoader()) {
      return ToolProvider.findFirst("bach");
    }
    return ServiceLoader.load(ToolProvider.class, currentClassLoader).stream()
            .map(ServiceLoader.Provider::get)
            .filter(provider -> provider.name().equals("bach"))
            .findFirst();
  }

  @Test
  void findBachUsingServiceLoader() {
    assertTrue(findBach().isPresent(), "Service 'bach' not found!");
  }

  @Test
  void providerIsMemberOfNamedModule() {
    assertEquals("de.sormuras.bach", findBach().orElseThrow().getClass().getModule().getName());
  }

  @Test
  void runVersionYieldsZero() {
    var out = new PrintWriter(new StringWriter());
    var err = new PrintWriter(new StringWriter());
    assertEquals(0, findBach().orElseThrow().run(out, err, "version"));
  }

  @Test
  void runUnsupportedArgumentYieldsOne() {
    var out = new PrintWriter(new StringWriter());
    var err = new PrintWriter(new StringWriter());
    assertEquals(1, findBach().orElseThrow().run(out, err, "?"));
  }
}
