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

package build;

import de.sormuras.bach.Bach;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.project.Link;
import de.sormuras.bach.project.Project;

/** Bach's own build program. */
class Build {

  public static void main(String... args) {
    var project =
        Project.of("bach", Bach.VERSION)
            .withCompileMainSourcesForJavaRelease(11)
            .withMainSource("src/de.sormuras.bach/main/java")
            .withTestSource("src/de.sormuras.bach/test/java-module")
            .withTestSource("src/test.base/test/java")
            .withTestSource("src/test.modules/test/java")
            .withPreview("src/test.preview/test-preview/java")
            .with(
                Library.of()
                    .withRequires("org.junit.platform.console")
                    .with(
                        Link.ofJUnitPlatform("commons", "1.7.0-M1"),
                        Link.ofJUnitPlatform("console", "1.7.0-M1"),
                        Link.ofJUnitPlatform("engine", "1.7.0-M1"),
                        Link.ofJUnitPlatform("launcher", "1.7.0-M1"),
                        Link.ofJUnitPlatform("reporting", "1.7.0-M1"),
                        Link.ofJUnitPlatform("testkit", "1.7.0-M1"))
                    .with(
                        Link.ofJUnitJupiter("", "5.7.0-M1"),
                        Link.ofJUnitJupiter("api", "5.7.0-M1"),
                        Link.ofJUnitJupiter("engine", "5.7.0-M1"),
                        Link.ofJUnitJupiter("params", "5.7.0-M1"))
                    .with(
                        Link.ofCentral(
                            "org.apiguardian.api", "org.apiguardian:apiguardian-api:1.1.0"),
                        Link.ofCentral("org.opentest4j", "org.opentest4j:opentest4j:1.2.0")));

    Bach.ofSystem().with(System.Logger.Level.ALL).with(project).buildProject();
  }
}
