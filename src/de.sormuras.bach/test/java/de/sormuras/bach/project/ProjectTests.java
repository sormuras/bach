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
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void touch() {
    var project =
        Project.of()
            .version("11.3")
            .library(
                Library.of()
                    .requires("org.junit.platform.console")
                    .map("se.jbee.inject")
                    .toJitPack("jbee", "silk", "master-SNAPSHOT")
                    .apply(library -> mapJUnitJupiterModules(library, "5.7.0-M1"))
                    .apply(library -> mapJUnitVintageModules(library, "5.7.0-M1"))
                    .apply(library -> mapJUnitPlatformModules(library, "1.7.0-M1")));

    assertEquals("11.3", project.version().toString());

    var silk = project.library().findUri("se.jbee.inject").orElseThrow();
    assertEquals("https", silk.getScheme());
    assertEquals("jitpack.io", silk.getHost());
    assertEquals("silk-master-SNAPSHOT.jar", Path.of(silk.getPath()).getFileName().toString());

    var junit4 = project.library().findUri("junit").orElseThrow();
    assertTrue(junit4.toString().endsWith("4.13.jar"), junit4 + " ends with `4.13.jar`");
  }

  private Library mapJUnitJupiterModules(Library library, String version) {
    var suffixes = Set.of("", ".api", ".engine", ".params");
    for (var suffix : suffixes) library = mapJUnitJupiterModule(library, suffix, version);
    return library;
  }

  private Library mapJUnitJupiterModule(Library library, String suffix, String version) {
    var module = "org.junit.jupiter" + suffix;
    var artifact = "junit-jupiter" + suffix.replace('.', '-');
    return library.map(module).toMavenCentral("org.junit.jupiter", artifact, version);
  }

  private Library mapJUnitPlatformModules(Library library, String version) {
    var suffixes = Set.of(".commons", ".console", ".engine", ".launcher", ".reporting", ".testkit");
    for (var suffix : suffixes) library = mapJUnitPlatformModule(library, suffix, version);
    return library;
  }

  private Library mapJUnitPlatformModule(Library library, String suffix, String version) {
    var module = "org.junit.platform" + suffix;
    var artifact = "junit-platform" + suffix.replace('.', '-');
    return library.map(module).toMavenCentral("org.junit.platform", artifact, version);
  }

  private Library mapJUnitVintageModules(Library library, String version) {
    return library
        .map("org.junit.vintage.engine")
        .toMavenCentral("org.junit.vintage", "junit-vintage-engine", version)
        .map("junit")
        .toMavenCentral("junit", "junit", "4.13")
        .map("org.hamcrest")
        .toMavenCentral("org.hamcrest", "hamcrest", "2.2");
  }
}
