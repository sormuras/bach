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

import java.nio.file.Path;
import java.util.Collection;

/*BODY*/
public /*STATIC*/ class Jigsaw {

  private final Bach bach;
  private final Project project;
  private final Project.Realm realm;
  private final Project.Target target;
  private final Path classes;

  public Jigsaw(Bach bach, Project project, Project.Realm realm) {
    this.bach = bach;
    this.project = project;
    this.realm = realm;
    this.target = project.target(realm);
    this.classes = target.directory.resolve("jigsaw").resolve("classes");
  }

  public void compile(Collection<String> modules) {
    bach.log("Compiling %s realm jigsaw modules: %s", realm.name, modules);
    bach.run(
        new Command("javac")
            .addEach(realm.toolArguments.javac)
            .add("-d", classes)
            .addIff(realm.preview, "--enable-preview")
            .addIff(realm.release != 0, "--release", realm.release)
            .add("--module-path", project.modulePaths(target))
            .add("--module-source-path", realm.moduleSourcePath)
            .add("--module-version", project.version)
            .add("--module", String.join(",", modules)));
    for (var module : modules) {
      var unit = realm.unit(module).orElseThrow();
      jarModule(unit);
      jarSources(unit);
    }
  }

  private void jarModule(Project.ModuleUnit unit) {
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
