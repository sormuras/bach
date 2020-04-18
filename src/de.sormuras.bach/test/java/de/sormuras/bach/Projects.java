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

import de.sormuras.bach.project.Information;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.project.Structure;
import de.sormuras.bach.project.Directory;
import de.sormuras.bach.project.Realm;
import de.sormuras.bach.project.Unit;
import de.sormuras.bach.tool.JavaCompiler;
import de.sormuras.bach.tool.Tool;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/** Projects. */
public interface Projects {

  final class Example {

    private final Project project;
    private final Map<Path, String> files;

    public Example(Project project, Map<Path, String> files) {
      this.project = project;
      this.files = files;
    }

    public Project project() {
      return project;
    }

    Path deploy(Path base) throws Exception {
      if (Files.exists(base)) throw new IllegalArgumentException("base directory must not exist");
      Files.createDirectories(base);
      for (var entry : files.entrySet()) {
        var path = base.resolve(entry.getKey());
        var string = path.toString();
        var isDirectory = string.endsWith("/") || string.endsWith("\\");
        if (isDirectory) Files.createDirectories(path);
        else {
          Files.createDirectories(path.getParent());
          Files.writeString(path, entry.getValue());
        }
      }
      return base;
    }
  }

  static Example exampleOfJigsawQuickStartGreetings(Workspace workspace) {
    var src = workspace.base().resolve("src");
    var module = ModuleDescriptor.newModule("com.greetings").mainClass("com.greetings.Main");
    var directory = new Directory(src.resolve("com.greetings"), Directory.Type.SOURCE, 0);
    var unit = new Unit(module.build(), List.of(directory));
    var javac =
        Tool.javac(
            List.of(
                new JavaCompiler.CompileModulesCheckingTimestamps(List.of("com.greetings")),
                new JavaCompiler.ModuleSourcePathInModulePatternForm(List.of(src.toString())),
                new JavaCompiler.DestinationDirectory(workspace.classes("", 0))));
    var realm = new Realm("", List.of(unit), "com.greetings", List.of(), javac);
    return new Example(
        new Project(
            "Jigsaw Quick Start: Greetings",
            Version.parse("1"),
            new Information(
                "This example is a module named com.greetings that simply prints \"Greetings!\"."
                    + " The module consists of two source files: the module declaration and the main class.",
                URI.create("https://openjdk.java.net/projects/jigsaw/quick-start")),
            new Structure(List.of(realm), "", Library.of())),
        Map.of(
            Path.of("src/com.greetings", "module-info.java"),
            "module com.greetings {}\n",
            Path.of("src/com.greetings/com/greetings", "Main.java"),
            "package com.greetings;\n"
                + "public class Main {\n"
                + "  public static void main(String[] args) {\n"
                + "    System.out.println(\"Greetings!\");\n"
                + "  }\n"
                + "}\n"));
  }

  static Example exampleOfSingleton(Workspace workspace, String... requires) {
    var source = new StringJoiner("\n", "module singleton {", "}\n");
    var module = ModuleDescriptor.newModule("singleton");
    for (var require : requires) {
      source.add("  requires " + require + ";");
      module.requires(require);
    }

    return new Example(
        new Project(
            "Singleton",
            Version.parse("0"),
            Information.of(),
            new Structure(
                List.of(
                    new Realm(
                        "",
                        List.of(
                            new Unit(
                                module.build(),
                                List.of(
                                    new Directory(workspace.base(), Directory.Type.SOURCE, 0)))),
                        "",
                        List.of(),
                        Tool.javac(
                            List.of(
                                new JavaCompiler.CompileModulesCheckingTimestamps(
                                    List.of("singleton")),
                                new JavaCompiler.ModuleSourcePathInModuleSpecificForm(
                                    "singleton", List.of(workspace.base())),
                                new JavaCompiler.DestinationDirectory(workspace.classes("", 0)),
                                new JavaCompiler.ModulePath(List.of(workspace.lib())))))),
                "",
                Library.of())),
        Map.of(Path.of("module-info.java"), source.toString()));
  }
}
