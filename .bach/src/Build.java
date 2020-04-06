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

import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

/** Bach's own build program. */
class Build {

  public static void main(String... args) {
    var bach = new Bach(Bach.Printer.ofSystem(Level.ALL), Bach.Workspace.of());
    bach.build(project("Bach.java", "11.0-ea"));
  }

  static Bach.Project project(String name, String version) {
    return new Bach.Project(
        name,
        Version.parse(version),
        new Bach.Information(
            "\uD83C\uDFBC Java Shell Builder - Build modular Java projects with JDK tools",
            URI.create("https://github.com/sormuras/bach")),
        new Bach.Structure(List.of(mainRealm(), testRealm()), "main"));
  }

  static Bach.Realm mainRealm() {
    return new Bach.Realm(
        "main",
        11,
        false,
        List.of(
            new Bach.Unit(
                ModuleDescriptor.newModule("de.sormuras.bach").build(),
                Bach.Directory.listOf(Path.of("src/de.sormuras.bach/main")))));
  }

  static Bach.Realm testRealm() {
    return new Bach.Realm(
        "test",
        11,
        false,
        List.of(
            //
            new Bach.Unit(
                ModuleDescriptor.newOpenModule("de.sormuras.bach").build(),
                Bach.Directory.listOf(Path.of("src/de.sormuras.bach/test"))),
            //
            new Bach.Unit(
                ModuleDescriptor.newOpenModule("test.base").build(),
                Bach.Directory.listOf(Path.of("src/test.base/test"))),
            //
            new Bach.Unit(
                ModuleDescriptor.newOpenModule("test.modules").build(),
                Bach.Directory.listOf(Path.of("src/test.modules/test")))));
  }
}
