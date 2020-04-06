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
import de.sormuras.bach.project.Structure;
import de.sormuras.bach.project.structure.Directory;
import de.sormuras.bach.project.structure.Realm;
import de.sormuras.bach.project.structure.Unit;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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

  static Example exampleOfJigsawQuickStartGreetings() {
    var module = ModuleDescriptor.newModule("com.greetings").mainClass("com.greetings.Main");
    var directory = new Directory(Path.of("src/com.greetings"), Directory.Type.SOURCE, 0);
    var unit = new Unit(module.build(), List.of(directory));
    var realm = new Realm("", 0, false, List.of(unit));
    return new Example(
        new Project(
            "Jigsaw Quick Start: Greetings",
            Version.parse("1"),
            new Information(
                "This example is a module named com.greetings that simply prints \"Greetings!\"."
                    + " The module consists of two source files: the module declaration and the main class.",
                URI.create("https://openjdk.java.net/projects/jigsaw/quick-start")),
            new Structure(List.of(realm))),
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
}
