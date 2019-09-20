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

public class Jigsaw {

  private final Bach bach;
  private final Project project;

  public Jigsaw(Bach bach, Project project) {
    this.bach = bach;
    this.project = project;
    bach.log("New Jigsaw created");
  }

  public List<Command> toCommands(Project.Realm realm, Collection<String> modules) {
    bach.log("Generating commands for compiling %s realm: %s", realm.name, modules);
    var target = project.targetDirectory.resolve("realm").resolve(realm.name);
    var exploded = target.resolve("exploded");
    var destination = exploded.resolve("jigsaw");
    var javac =
        new Command("javac")
            .add("-d", destination)
            .addIff(realm.preview, "--enable-preview")
            .addIff(realm.release != 0, "--release", realm.release)
            .add("--module-path", project.library.modulePaths)
            .add("--module-source-path", realm.moduleSourcePath)
            .add("--module-version", project.version)
            .add("--module", String.join(",", modules));

    var commands = new ArrayList<>(List.of(javac));
    for (var module : modules) {
      var unit = realm.modules.get(module);
      var version = unit.descriptor.version();
      var jarFile = module + "-" + version.orElse(project.version);
      var modulesDirectory = Util.treeCreate(target.resolve("modules"));
      var jar = modulesDirectory.resolve(jarFile + ".jar");

      commands.add(
          new Command("jar")
              .add("--create")
              .add("--file", jar)
              .addIff(bach.verbose(), "--verbose")
              .addIff("--module-version", version)
              .addIff("--main-class", unit.descriptor.mainClass())
              .add("-C", destination.resolve(module))
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

      var sourcesJar = Util.treeCreate(target.resolve("sources")).resolve(jarFile + "-sources.jar");
      commands.add(
          new Command("jar")
              .add("--create")
              .add("--file", sourcesJar)
              .addIff(bach.verbose(), "--verbose")
              .add("--no-manifest")
              .addEach(unit.sources, (cmd, path) -> cmd.add("-C", path).add("."))
              .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add(".")));
    }

    commands.add(
        new Command("javadoc")
            .add("-d", exploded.resolve("javadoc"))
            .add("-encoding", "UTF-8")
            .addIff(!bach.verbose(), "-quiet")
            .add("-Xdoclint:-missing")
            .add("-windowtitle", project.name)
            .add("--module-path", project.library.modulePaths)
            .add("--module-source-path", realm.moduleSourcePath)
            .add("--module", String.join(",", modules)));
    var javadocJar = Util.treeCreate(target.resolve("javadoc")).resolve("all-javadoc.jar");
    commands.add(
        new Command("jar")
            .add("--create")
            .add("--file", javadocJar)
            .addIff(bach.verbose(), "--verbose")
            .add("--no-manifest")
            .add("-C", exploded.resolve("javadoc"))
            .add("."));

    return List.copyOf(commands);
  }
}
