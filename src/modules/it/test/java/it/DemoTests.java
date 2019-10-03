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
import static org.junit.jupiter.api.Assertions.fail;

import de.sormuras.bach.Project;
import java.io.File;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class DemoTests {

  @Test
  void build() {
    var demo = Path.of("demo/src/de.sormuras.bach.demo/main/java");
    var multi = Path.of("demo/src/de.sormuras.bach.demo.multi/main");
    var main =
        new Project.Realm(
            "main",
            false,
            11,
            String.join(
                File.pathSeparator,
                String.join(File.separator, "demo", "src", "*", "main", "java"),
                String.join(File.separator, "demo", "src", "*", "main", "java-9")),
            Project.ToolArguments.of(),
            List.of(
                Project.ModuleUnit.of(demo),
                new Project.ModuleUnit(
                    Project.ModuleInfo.of(multi.resolve("java-9/module-info.java")),
                    List.of(
                        Project.Source.of(multi.resolve("java-8"), 8),
                        new Project.Source(multi.resolve("java-9"), 9, Set.of()),
                        Project.Source.of(multi.resolve("java-11"), 11)),
                    List.of(),
                    null)));

    var integration = Project.ModuleUnit.of(Path.of("demo/src/integration/test/java"));
    var demoPath = Path.of("demo/src/de.sormuras.bach.demo/test");
    var demoTest =
        new Project.ModuleUnit(
            Project.ModuleInfo.of(demoPath.resolve("module/module-info.java")),
            List.of(Project.Source.of(demoPath.resolve("java"))),
            List.of(), // resources
            null);
    var test = Project.Realm.of("test", List.of(integration, demoTest), main);

    assertEquals(
        String.join(
            File.pathSeparator,
            String.join(File.separator, "demo", "src", "*", "test", "java"),
            String.join(File.separator, "demo", "src", "*", "test", "module")),
        test.moduleSourcePath);

    var library = new Project.Library(Path.of("demo/lib"));
    var project =
        new Project(
            Path.of("demo"),
            Path.of("demo/bin"),
            "de.sormuras.bach.demo",
            Version.parse("1"),
            library,
            List.of(main, test));

    var bach = new Probe(project);
    try {
      bach.build();
    } catch (Throwable t) {
      bach.lines().forEach(System.out::println);
      fail(t);
    } finally {
      bach.errors().forEach(System.err::println);
    }
  }
}
