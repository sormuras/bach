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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void test() {
    var project =
        Project.of("project", "11.3")
            .with(Locator.ofJitPack("se.jbee.inject", "jbee", "silk", "master-SNAPSHOT"))
            .with(new JUnitPlatform())
            .with(new JUnitJupiter())
            .with(new JUnitVintage());

    assertEquals("project 11.3", project.toNameAndVersion());

    var basics = project.basics();
    assertEquals("11.3", basics.version().toString());

    var silk = project.findModuleUri("se.jbee.inject").orElseThrow();
    assertEquals("https", silk.getScheme());
    assertEquals("jitpack.io", silk.getHost());
    assertEquals("silk-master-SNAPSHOT.jar", Path.of(silk.getPath()).getFileName().toString());

    var junit4 = project.findModuleUri("junit").orElseThrow();
    assertTrue(junit4.toString().endsWith("4.13.jar"), junit4 + " ends with `4.13.jar`");
  }

  static class JUnitPlatform extends TreeSet<Locator> {

    final String version = "1.7.0-M1";

    JUnitPlatform() {
      var suffixes = Set.of("commons", "console", "engine", "launcher", "reporting", "testkit");
      for (var suffix : suffixes) add(locator('.' + suffix));
    }

    private Locator locator(String suffix) {
      var module = "org.junit.platform" + suffix;
      var artifact = "junit-platform" + suffix.replace('.', '-');
      return Locator.ofCentral(module, "org.junit.platform", artifact, version);
    }
  }

  static class JUnitJupiter extends TreeSet<Locator> {

    final String version = "5.7.0-M1";

    JUnitJupiter() {
      var suffixes = Set.of("", ".api", ".engine", ".params");
      for (var suffix : suffixes) add(locator(suffix));
    }

    private Locator locator(String suffix) {
      var module = "org.junit.jupiter" + suffix;
      var artifact = "junit-jupiter" + suffix.replace('.', '-');
      return Locator.ofCentral(module, "org.junit.jupiter", artifact, version);
    }
  }

  static class JUnitVintage extends TreeSet<Locator> {
    {
      add(Locator.ofCentral("junit", "junit", "junit", "4.13"));
      add(Locator.ofCentral("org.hamcrest", "org.hamcrest", "hamcrest", "2.2"));
      add(
          Locator.ofCentral(
              "org.junit.vintage.engine", "org.junit.vintage", "junit-vintage-engine", "5.7.0-M1"));
    }
  }
}
