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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import de.sormuras.bach.Command;
import de.sormuras.bach.MultiModuleCompiler;
import de.sormuras.bach.Domain;
import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DemoTests {

  @Test
  void createBuildBatch() throws Exception {
    var main =
        new Domain.MainRealm(
            String.join(File.separator, "demo", "src", "*", "main", "java"),
            Map.of(
                "de.sormuras.bach.demo",
                new Domain.ModuleSource(
                    Path.of("demo/src/de.sormuras.bach.demo/main/java/module-info.java"),
                    Path.of("demo/src/de.sormuras.bach.demo/main/java"),
                    Path.of("demo/src/de.sormuras.bach.demo/main/resources"),
                    ModuleDescriptor.newModule("de.sormuras.bach.demo").version("1").build())));
    var test =
        new Domain.TestRealm(
            main,
            String.join(File.separator, "demo", "src", "*", "test", "java"),
            Map.of(
                "it",
                new Domain.ModuleSource(
                    Path.of("demo/src/it/test/java/module-info.java"),
                    Path.of("demo/src/it/test/java"),
                    Path.of("demo/src/it/test/resources"),
                    ModuleDescriptor.newOpenModule("it")
                        .requires("de.sormuras.bach.demo")
                        .build())));
    var library = new Domain.Library(List.of(Path.of("demo/lib")), __ -> null);
    var project =
        new Domain.Project(
            "demo", Version.parse("23"), library, List.of(main, test), Path.of("demo/bin"));

    var commands = new MultiModuleCompiler(project).toCommands(main, main.modules.keySet());
    var javac = commands.get(0);
    assertEquals("javac", javac.getName());
    assertFalse(javac.toCommandLine().isBlank());
    assertLinesMatch(
        List.of(
            "-d",
            Path.of("demo/bin/realm/main/exploded/multi-module").toString(),
            "--module-source-path",
            String.join(File.separator, "demo", "src", "*", "main", "java"),
            "--module-version",
            "23",
            "--module",
            "de.sormuras.bach.demo"),
        javac.getList());

    Files.write(
        Path.of("bin", "build-demo.bat"),
        commands.stream().map(Command::toCommandLine).collect(Collectors.toList()));
  }

  @Test
  void build() {
    var bach = new Probe(Path.of("demo"));
    assertDoesNotThrow(bach::build, "bach::build failed: " + bach.lines());
    assertLinesMatch(List.of(">> BUILD >>"), bach.lines());
  }
}
