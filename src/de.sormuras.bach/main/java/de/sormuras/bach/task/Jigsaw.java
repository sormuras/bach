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

package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Call;
import de.sormuras.bach.project.Folder;
import de.sormuras.bach.project.Realm;
import de.sormuras.bach.project.Source;
import de.sormuras.bach.project.Unit;
import de.sormuras.bach.util.Paths;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class Jigsaw {

  private final Bach bach;
  private final Realm realm;
  private final Folder folder;
  private final Path classesDirectory;
  private final Path javadocDirectory;

  Jigsaw(Bach bach, Realm realm) {
    this.bach = bach;
    this.realm = realm;
    this.folder = bach.getProject().folder();
    this.classesDirectory = folder.realm(realm.name()).resolve("classes/jigsaw");
    this.javadocDirectory = folder.realm(realm.name()).resolve("javadoc/jigsaw");
  }

  void compile(List<Unit> units) {
    var allModuleNames = units.stream().map(Unit::name).collect(Collectors.joining(","));
    var normalUnits = new ArrayList<Unit>();
    var normalNames =
        units.stream()
            .filter(Predicate.not(Unit::isMultiRelease))
            .peek(normalUnits::add)
            .map(Unit::name)
            .collect(Collectors.joining(","));
    var modulePath = new ArrayList<Path>();
    modulePath.add(folder.modules(realm.name())); // current realm first, like "main/modules"...
    modulePath.addAll(realm.modulePaths()); // dependencies last, like "lib"...
    if (!normalNames.isBlank()) {
      bach.getLog().info("Compiling %d module(s)...", normalUnits.size());
      bach.execute(
          new Call("javac")
              .add("-d", classesDirectory)
              .iff(realm.release(), (call, release) -> call.add("--release", release))
              .iff(realm.isPreviewRealm(), call -> call.add("--enable-preview"))
              .add("--module", normalNames)
              .add("--module-source-path", realm.moduleSourcePath())
              .forEach(units, this::patchModule)
              .add("--module-path", Paths.filterExisting(modulePath))
              .add("--module-version", bach.getProject().version())
              .forEach(realm.argumentsFor("javac"), Call::add)
              .add("--class-path", ""));
    }

    var javadocModulesOption = realm.argumentsFor(Realm.JAVADOC_MODULES_OPTION);
    var javadocModules =
        javadocModulesOption.isEmpty() || javadocModulesOption.equals(List.of(Realm.ALL_MODULES))
            ? allModuleNames
            : String.join(",", javadocModulesOption);
    if (realm.isMainRealm()) {
      var allModuleSourcePaths =
          units.stream()
              .map(unit -> Paths.star(unit.info(), unit.name()))
              .distinct()
              .collect(Collectors.joining(File.pathSeparator));

      var javadoc =
          new Call("javadoc")
              .forEach(realm.argumentsFor("javadoc"), Call::add)
              .add("-d", Paths.createDirectories(javadocDirectory))
              .add("--module", javadocModules)
              .iff(!bach.isVerbose(), c -> c.add("-quiet"))
              .add("--module-path", Paths.filterExisting(modulePath))
              .add("--module-source-path", allModuleSourcePaths);
      for (var unit : units) {
        if (unit.isMultiRelease()) {
          var base = unit.sources().get(0);
          if (!unit.info().startsWith(base.path())) {
            javadoc.add("--patch-module", unit.name() + "=" + base.path());
          }
        }
      }
      bach.execute(javadoc);
      var javadocJar = bach.getProject().javadocJar(realm);
      Paths.createDirectories(javadocJar.getParent());
      bach.execute(
          new Call("jar")
              .add("--create")
              .add("--file", javadocJar)
              .iff(bach.isVerbose(), c -> c.add("--verbose"))
              .add("--no-manifest")
              .add("-C", javadocDirectory)
              .add("."));
    }

    Paths.createDirectories(folder.modules(realm.name()));
    for (var unit : units) {
      if (unit.isMultiRelease()) continue;
      jarModule(unit);
      if (realm.isMainRealm()) {
        jarSources(unit);
      }
    }

    if (bach.isVerbose()) {
      var jdeps =
          new Call("jdeps")
              .add("--module-path", Paths.filterExisting(modulePath))
              .add("--multi-release", "BASE");
      var jdepsExitCode =
          bach.run(
              jdeps
                  .clone()
                  .add("-summary")
                  .add("--dot-output", folder.realm(realm.name(), "dot"))
                  .add("--add-modules", allModuleNames));
      if (jdepsExitCode != 0) bach.getLog().warning("jdeps reported: " + jdepsExitCode);
      else bach.execute(jdeps.clone().add("--check", javadocModules));
    }
  }

  private void patchModule(Call call, Unit unit) {
    if (unit.patches().isEmpty()) return;
    call.add("--patch-module", unit.name() + '=' + Paths.join(unit.patches()));
  }

  private void jarModule(Unit unit) {
    var file = bach.getProject().modularJar(unit); // "../{REALM}/modules/{MODULE}-{VERSION}.jar"
    var resources = Paths.filterExisting(unit.resources());
    bach.execute(
        new Call("jar")
            .add("--create")
            .add("--file", file)
            .iff(bach.isVerbose(), c -> c.add("--verbose"))
            .iff(unit.descriptor().version(), (c, v) -> c.add("--module-version", v.toString()))
            .iff(unit.descriptor().mainClass(), (c, m) -> c.add("--main-class", m))
            .add("-C", classesDirectory.resolve(unit.name()))
            .add(".")
            .forEach(resources, (cmd, path) -> cmd.add("-C", path).add(".")));
    if (bach.isVerbose()) {
      bach.execute("jar", "--describe-module", "--file", file);
    }
  }

  private void jarSources(Unit unit) {
    var file = bach.getProject().sourcesJar(unit); // "../{REALM}/{MODULE}-{VERSION}-sources.jar"
    Paths.createDirectories(file.getParent());
    var sources = Paths.filterExisting(unit.sources(Source::path));
    var resources = Paths.filterExisting(unit.resources());
    bach.execute(
        new Call("jar")
            .add("--create")
            .add("--file", file)
            .iff(bach.isVerbose(), c -> c.add("--verbose"))
            .add("--no-manifest")
            .forEach(sources, (cmd, path) -> cmd.add("-C", path).add("."))
            .forEach(resources, (cmd, path) -> cmd.add("-C", path).add(".")));
  }
}
