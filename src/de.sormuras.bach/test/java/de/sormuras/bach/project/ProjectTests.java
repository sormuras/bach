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
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void test() {
    var project =
        Project.of("project", "11.3")
            .with(Locator.ofJitPack("se.jbee.inject", "jbee", "silk", "master-SNAPSHOT"))
            .with(
                Locator.ofJUnitPlatform("commons", "1.7.0-M1"),
                Locator.ofJUnitPlatform("console", "1.7.0-M1"),
                Locator.ofJUnitPlatform("engine", "1.7.0-M1"),
                Locator.ofJUnitPlatform("launcher", "1.7.0-M1"),
                Locator.ofJUnitPlatform("reporting", "1.7.0-M1"),
                Locator.ofJUnitPlatform("testkit", "1.7.0-M1"))
            .with(
                Locator.ofJUnitJupiter("", "5.7.0-M1"),
                Locator.ofJUnitJupiter("api", "5.7.0-M1"),
                Locator.ofJUnitJupiter("engine", "5.7.0-M1"),
                Locator.ofJUnitJupiter("params", "5.7.0-M1"))
            .with(
                Locator.ofCentral("junit", "junit", "junit", "4.13"),
                Locator.ofCentral("org.hamcrest", "org.hamcrest", "hamcrest", "2.2"),
                Locator.ofCentral(
                        "org.junit.vintage.engine",
                        "org.junit.vintage:junit-vintage-engine:5.7.0-M1")
                    .withVersion("5.7-M1")
                    .withSize(63969)
                    .withDigest("md5", "455be2fc44c7525e7f20099529aec037"));

    assertEquals("project 11.3", project.toNameAndVersion());

    var basics = project.basics();
    assertEquals("11.3", basics.version().toString());

    var silk = project.findLocator("se.jbee.inject").map(Locator::toURI).orElseThrow();
    assertEquals("https", silk.getScheme());
    assertEquals("jitpack.io", silk.getHost());
    assertEquals("silk-master-SNAPSHOT.jar", Path.of(silk.getPath()).getFileName().toString());

    var junit4 = project.findLocator("junit").map(Locator::toURI).orElseThrow();
    assertTrue(junit4.toString().endsWith("4.13.jar"), junit4 + " ends with `4.13.jar`");

    var vintage = project.findLocator("org.junit.vintage.engine").orElseThrow();
    assertEquals("5.7-M1", vintage.findVersion().orElseThrow());
    assertEquals(63969, vintage.findSize().orElseThrow());
    assertEquals(Map.of("md5", "455be2fc44c7525e7f20099529aec037"), vintage.findDigests());
  }

  @Test
  void buildDocProjectJigsawQuickStart() {
    var paths = Base.of("doc/project", "JigsawQuickStart");
    var project =
        Project.of("greetings", "1")
            .with(paths)
            .with(MainSources.of().with(ModuleSource.of(paths.directory("com.greetings"))));

    var lines = new ArrayList<String>();
    var bach =
        Bach.of(project)
            .with(new Bach.Logbook(string -> string.lines().forEach(lines::add), Level.ALL));

    bach.build();

    assertLinesMatch(
        List.of(">> BEGIN >>", "project greetings {", ">>>>", "}", ">> END. >>"), lines);
  }
}
