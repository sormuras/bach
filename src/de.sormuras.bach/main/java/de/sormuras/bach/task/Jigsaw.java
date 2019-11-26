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
    var normalNames =
        units.stream()
            .filter(Predicate.not(Unit::isMultiRelease))
            .map(Unit::name)
            .collect(Collectors.joining(","));
    var modulePaths = Paths.filterExisting(realm.modulePaths());
    if (!normalNames.isBlank()) {
      bach.execute(
          new Call("javac")
              .forEach(realm.argumentsFor("javac"), Call::add)
              .add("-d", classesDirectory)
              .add("--module", normalNames)
              .add("--module-source-path", realm.moduleSourcePath())
              .forEach(units, this::patchModule)
              .add("--module-path", modulePaths)
              .add("--module-version", bach.getProject().version()));
    }

    if (realm.isDeployRealm()) {
      var allModuleNames = units.stream().map(Unit::name).collect(Collectors.joining(","));
      var allModuleSourcePaths =
          units.stream()
              .map(unit -> unit.info().getParent().toString().replace(unit.name(), "*"))
              .distinct()
              .collect(Collectors.joining(File.pathSeparator));
      var javadoc =
          new Call("javadoc")
              .add("-d", Paths.createDirectories(javadocDirectory))
              .add("--module", allModuleNames)
              .add("-encoding", "UTF-8")
              .add("-locale", "en")
              .iff(!bach.isVerbose(), c -> c.add("-quiet"))
              .add("-Xdoclint:-missing")
              .add("--module-path", modulePaths)
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
      bach.execute(
          new Call("jar")
              .add("--create")
              .add("--file", bach.getProject().javadocJar(realm))
              .iff(bach.isVerbose(), c -> c.add("--verbose"))
              .add("--no-manifest")
              .add("-C", javadocDirectory)
              .add("."));
    }

    Paths.createDirectories(folder.modules(realm.name()));
    for (var unit : units) {
      if (unit.isMultiRelease()) continue;
      jarModule(unit);
      if (realm.isDeployRealm()) {
        jarSources(unit);
      }
    }
  }

  private void patchModule(Call call, Unit unit) {
    if (unit.patches().isEmpty()) return;
    call.add("--patch-module", unit.name() + '=' + Paths.join(unit.patches()));
  }

  private void jarModule(Unit unit) {
    var file = bach.getProject().modularJar(unit); // "../{REALM}/modules/{MODULE}-{VERSION}.jar"
    var resources = Paths.filterExisting(unit.resources()); // TODO include patched resources
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
      bach.execute(new Call("jar").add("--describe-module").add("--file", file));
    }
  }

  private void jarSources(Unit unit) {
    var file = bach.getProject().sourcesJar(unit); // "../{REALM}/{MODULE}-{VERSION}-sources.jar"
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
