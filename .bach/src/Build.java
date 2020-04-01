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

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;

/** Bach's own build program. */
class Build {

  public static void main(String... args) {
    var project = project("Bach.java", "11.0-ea");
    var bach = new Bach();
    bach.print("Build %s using %s", project.toNameAndVersion(), bach);
    bach.print("%s", project);
    // bach.build(project).assertSuccessful();
  }

  private static Bach.Project project(String name, String version) {
    return new Bach.Project(
        name,
        Version.parse(version),
        new Bach.Structure(List.of(mainRealm(), testRealm(), testPreviewRealm())));
  }

  private static Bach.ModuleCollection mainRealm() {
    return new Bach.ModuleCollection(
        "main",
        11,
        false,
        List.of(
            new Bach.ModuleDescription(
                ModuleDescriptor.newModule("de.sormuras.bach").build(),
                List.of(new Bach.Directory(Path.of("src/de.sormuras.bach/main/java"), 0))) //
            ) //
        );
  }

  private static Bach.ModuleCollection testRealm() {
    return new Bach.ModuleCollection(
        "test",
        11,
        false,
        List.of(
            //
            new Bach.ModuleDescription(
                ModuleDescriptor.newOpenModule("de.sormuras.bach").build(),
                List.of(
                    new Bach.Directory(Path.of("src/de.sormuras.bach/test/java"), 0),
                    new Bach.Directory(Path.of("src/de.sormuras.bach/test/java-module"), 0))),
            //
            new Bach.ModuleDescription(
                ModuleDescriptor.newOpenModule("test.base").build(),
                List.of(new Bach.Directory(Path.of("src/test.base/test/java"), 0))),
            //
            new Bach.ModuleDescription(
                ModuleDescriptor.newOpenModule("test.modules").build(),
                List.of(new Bach.Directory(Path.of("src/test.modules/test/java"), 0)))
            //
            ));
  }

  private static Bach.ModuleCollection testPreviewRealm() {
    var descriptor = ModuleDescriptor.newOpenModule("test.preview").build();
    var release = Runtime.version().feature();
    var directory = new Bach.Directory(Path.of("src/test.preview/test-preview/java"), release);
    var unit = new Bach.ModuleDescription(descriptor, List.of(directory));
    return new Bach.ModuleCollection("test-preview", release, true, List.of(unit));
  }
}
