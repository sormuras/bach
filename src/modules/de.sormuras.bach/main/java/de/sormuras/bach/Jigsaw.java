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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*BODY*/
public /*STATIC*/ class Jigsaw {

  private final Bach bach;
  private final Project project;
  private final Project.Realm realm;
  private final Project.Target target;

  public Jigsaw(Bach bach, Project project, Project.Realm realm) {
    this.bach = bach;
    this.project = project;
    this.realm = realm;
    this.target = project.target(realm);
  }

  public void compile(Collection<String> modules) {
    bach.log("Compiling %s realm jigsaw modules: %s", realm.name, modules);
    var classes = target.directory.resolve("jigsaw").resolve("classes");
    var modulePath = new ArrayList<Path>();
    if (Files.isDirectory(target.modules)) {
      modulePath.add(target.modules);
    }
    for (var other : realm.realms) {
      var otherTarget = project.target(other);
      if (Files.isDirectory(otherTarget.modules)) {
        modulePath.add(otherTarget.modules);
      }
    }
    modulePath.addAll(project.library.modulePaths);
    bach.run(
        new Command("javac")
            .add("-d", classes)
            .addIff(realm.preview, "--enable-preview")
            .addIff(realm.release != 0, "--release", realm.release)
            .add("--module-path", modulePath)
            .add("--module-source-path", realm.moduleSourcePath)
            .add("--module-version", project.version)
            .add("--module", String.join(",", modules)));
    for (var module : modules) {
      var unit = realm.units.get(module);
      jarModule(unit, classes);
      jarSources(unit);
    }
  }

  private void jarModule(Project.ModuleUnit unit, Path classes) {
    var descriptor = unit.info.descriptor();
    bach.run(
        new Command("jar")
            .add("--create")
            .add("--file", Util.treeCreate(target.modules).resolve(target.file(unit, ".jar")))
            .addIff(bach.verbose(), "--verbose")
            .addIff("--module-version", descriptor.version())
            .addIff("--main-class", descriptor.mainClass())
            .add("-C", classes.resolve(descriptor.name()))
            .add(".")
            .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add(".")));

    if (bach.verbose()) {
      bach.run(new Command("jar", "--describe-module", "--file", target.modularJar(unit)));
      var runtimeModulePath = new ArrayList<>(List.of(target.modules));
      for (var other : realm.realms) {
        var otherTarget = project.target(other);
        if (Files.isDirectory(otherTarget.modules)) {
          runtimeModulePath.add(otherTarget.modules);
        }
      }
      runtimeModulePath.addAll(project.library.modulePaths);
      bach.run(
          new Command("jdeps")
              .add("--module-path", runtimeModulePath)
              .add("--multi-release", "BASE")
              .add("--check", descriptor.name()));
    }
  }

  private void jarSources(Project.ModuleUnit unit) {
    bach.run(
        new Command("jar")
            .add("--create")
            .add("--file", target.sourcesJar(unit))
            .addIff(bach.verbose(), "--verbose")
            .add("--no-manifest")
            .addEach(unit.sources, (cmd, path) -> cmd.add("-C", path).add("."))
            .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add(".")));
  }
}
