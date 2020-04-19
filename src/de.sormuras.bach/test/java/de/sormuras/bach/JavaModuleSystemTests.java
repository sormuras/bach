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

import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

class JavaModuleSystemTests {

  @Test
  void runningOnTheModulePath() {
    var module = getClass().getModule();
    assertEquals("de.sormuras.bach", module.getName());
  }

  @Test
  void checkJUnitJupiterVersion() throws Exception {
    var expected = "5.6.2";
    // Java Class API
    if (Test.class.getPackage() != null) {
      var actual = Test.class.getPackage().getImplementationVersion();
      if (actual != null) {
        assertEquals(expected, actual);
      }
    }
    // Module System
    var jupiter = Test.class.getModule();
    if (jupiter.isNamed()) {
      var actual = jupiter.getDescriptor().version().orElseThrow().toString();
      assertEquals(expected, actual);
    }
    // JAR Manifest
    var location = Test.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    try (var jar = new JarFile(Path.of(location).toFile())) {
      var attributes = jar.getManifest().getMainAttributes();
      var actual = attributes.getValue("Implementation-Version");
      assertEquals(expected, actual);
    }
    // Reflection
    var engine = Class.forName("org.junit.jupiter.engine.JupiterTestEngine");
    if (!engine.getModule().isNamed()) {
      var method = engine.getMethod("getVersion");
      var result = method.invoke(engine.getConstructor().newInstance());
      @SuppressWarnings("unchecked")
      var actual = ((Optional<String>) result).orElseThrow();
      assertEquals(expected, actual);
    }
  }
}
