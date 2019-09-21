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

package de.sormuras.bach;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*BODY*/
public /*STATIC*/ class Jigsaw {

  private final Bach bach;
  private final Project project;

  public Jigsaw(Bach bach, Project project) {
    this.bach = bach;
    this.project = project;
    bach.log("New Jigsaw created");
  }

  public List<Command> toCommands(Project.Realm realm, Collection<String> modules) {
    bach.log("Generating commands for compiling %s realm: %s", realm.name, modules);
    var commands = new ArrayList<Command>();

    var targetDirectory = project.targetDirectory.resolve("realm").resolve(realm.name);
    var modulesDirectory = targetDirectory.resolve("modules");
    var jigsawDirectory = modulesDirectory.resolve("jigsaw");
    var classesDirectory = jigsawDirectory.resolve("classes");
    var javadocDirectory = jigsawDirectory.resolve("javadoc");

    commands.add(
        new Command("javac")
            .add("-d", classesDirectory)
            .addIff(realm.preview, "--enable-preview")
            .addIff(realm.release != 0, "--release", realm.release)
            .add("--module-path", project.library.modulePaths)
            .add("--module-source-path", realm.moduleSourcePath)
            .add("--module-version", project.version)
            .add("--module", String.join(",", modules)));

    for (var module : modules) {
      var unit = realm.modules.get(module);
      var version = unit.descriptor.version();
      var file = module + "-" + version.orElse(project.version);
      var jar = modulesDirectory.resolve(file + ".jar");

      commands.add(
          new Command("jar")
              .add("--create")
              .add("--file", jar)
              .addIff(bach.verbose(), "--verbose")
              .addIff("--module-version", version)
              .addIff("--main-class", unit.descriptor.mainClass())
              .add("-C", classesDirectory.resolve(module))
              .add(".")
              .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add(".")));

      if (bach.verbose()) {
        commands.add(new Command("jar", "--describe-module", "--file", jar));
        var runtimeModulePath = new ArrayList<>(List.of(modulesDirectory));
        runtimeModulePath.addAll(project.library.modulePaths);
        commands.add(
            new Command("jdeps")
                .add("--module-path", runtimeModulePath)
                .add("--multi-release", "BASE")
                .add("--check", module));
      }

      commands.add(
          new Command("jar")
              .add("--create")
              .add("--file", targetDirectory.resolve(file + "-sources.jar"))
              .addIff(bach.verbose(), "--verbose")
              .add("--no-manifest")
              .addEach(unit.sources, (cmd, path) -> cmd.add("-C", path).add("."))
              .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add(".")));
    }

    var nameDashVersion = project.name + '-' + project.version;
    commands.add(
        new Command("javadoc")
            .add("-d", javadocDirectory)
            .add("-encoding", "UTF-8")
            .addIff(!bach.verbose(), "-quiet")
            .add("-Xdoclint:-missing")
            .add("-windowtitle", "'API of " + nameDashVersion + "'")
            .add("--module-path", project.library.modulePaths)
            .add("--module-source-path", realm.moduleSourcePath)
            .add("--module", String.join(",", modules)));

    commands.add(
        new Command("jar")
            .add("--create")
            .add("--file", targetDirectory.resolve(nameDashVersion + "-javadoc.jar"))
            .addIff(bach.verbose(), "--verbose")
            .add("--no-manifest")
            .add("-C", javadocDirectory)
            .add("."));

    return List.copyOf(commands);
  }
}
