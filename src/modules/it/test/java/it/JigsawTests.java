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

import static java.lang.module.ModuleDescriptor.Requires.Modifier.STATIC;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import de.sormuras.bach.Command;
import de.sormuras.bach.Jigsaw;
import de.sormuras.bach.Project;
import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class JigsawTests {

  @Test
  void build() throws Exception {
    var main =
        new Project.Realm(
            "main",
            false,
            0,
            String.join(File.separator, "src", "modules", "*", "main", "java"),
            Map.of(
                "de.sormuras.bach",
                new Project.ModuleUnit(
                    Path.of("src/modules/de.sormuras.bach.demo/main/java/module-info.java"),
                    List.of(Path.of("src/modules/de.sormuras.bach/main/java")),
                    List.of(Path.of("src/modules/de.sormuras.bach/main/resources")),
                    ModuleDescriptor.newModule("de.sormuras.bach")
                        .requires("java.compiler")
                        .requires("java.net.http")
                        .requires(
                            Set.of(STATIC), "org.junit.platform.console", Version.parse("1.5.2"))
                        .build())));
    var test =
        new Project.Realm(
            "test",
            true,
            Runtime.version().feature(),
            String.join(File.separator, "src", "modules", "*", "test", "java"),
            Map.of(
                "it",
                new Project.ModuleUnit(
                    Path.of("src/modules/it/test/java/module-info.java"),
                    List.of(Path.of("src/modules/it/test/java")),
                    List.of(Path.of("src/modules/it/test/resources")),
                    ModuleDescriptor.newOpenModule("it")
                        .requires("de.sormuras.bach")
                        .requires("org.junit.jupiter")
                        .requires(
                            Set.of(STATIC), "org.junit.platform.console", Version.parse("1.5.2"))
                        .build())),
            main);

    var library = new Project.Library(List.of(Path.of("lib")), __ -> null);
    var project =
        new Project(
            Path.of(""),
            Path.of("bin"),
            "de.sormuras.bach",
            Version.parse("2-ea"),
            library,
            List.of(main, test));

    var bach = new Probe();
    var commands = new Jigsaw(bach, project).toCommands(main, main.modules.keySet());
    var actual = commands.stream().map(Command::toCommandLine).collect(Collectors.toList());
    assertLinesMatch(
        List.of(
            "javac -d bin.+classes --module-path lib --module-source-path src.+java --module-version 2.* --module de.sormuras.bach",
            "jar --create --file bin.+de.sormuras.bach-.*.jar --verbose -C bin.+de.sormuras.bach . -C src.+resources .",
            "jar --describe-module --file bin.+de.sormuras.bach-.*.jar",
            "jdeps --module-path bin.+modules;lib --multi-release BASE --check de.sormuras.bach",
            "jar --create --file bin.+de.sormuras.bach-2.*-sources.jar --verbose --no-manifest -C src.+java . -C src.+resources .",
            "javadoc -d bin.+javadoc -encoding UTF-8 -Xdoclint:-missing -windowtitle 'API of de.sormuras.bach-2.*' --module-path lib --module-source-path src.+java --module de.sormuras.bach",
            "jar --create --file bin.+bach-2.*-javadoc.jar --verbose --no-manifest -C bin.+javadoc ."),
        actual);
    Files.write(Path.of("bin", "build-bach.bat"), actual);

    for (var command : commands) {
      var code = bach.run(command).code;
      if (code != 0) {
        throw new AssertionError("Expected 0, but got: " + code);
      }
    }
  }
}
