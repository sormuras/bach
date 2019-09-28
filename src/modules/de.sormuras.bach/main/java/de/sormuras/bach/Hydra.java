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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/*BODY*/
/** Multi-release module compiler. */
public /*STATIC*/ class Hydra {

  private final Bach bach;
  private final Project project;
  private final Project.Realm realm;
  private final Project.Target target;
  private final Path classes;

  public Hydra(Bach bach, Project project, Project.Realm realm) {
    this.bach = bach;
    this.project = project;
    this.realm = realm;
    this.target = project.target(realm);
    this.classes = target.directory.resolve("hydra").resolve("classes");
  }

  public void compile(Collection<String> modules) {
    bach.log("Generating commands for %s realm multi-release modules(s): %s", realm.name, modules);
    for (var module : modules) {
      var unit = (Project.MultiReleaseUnit) realm.units.get(module);
      compile(unit);
    }
  }

  private void compile(Project.MultiReleaseUnit unit) {
    var sorted = new TreeSet<>(unit.releases.keySet());
    int base = sorted.first();
    bach.log("Base feature release number is: %d", base);

    for (int release : sorted) {
      compileRelease(unit, base, release);
    }
    jarModule(unit);
    jarSources(unit);
  }

  private void compileRelease(Project.MultiReleaseUnit unit, int base, int release) {
    var module = unit.info.descriptor().name();
    var source = unit.releases.get(release);
    var destination = classes.resolve(source.getFileName());
    var baseClasses = classes.resolve(unit.releases.get(base).getFileName()).resolve(module);
    var javac = new Command("javac").addIff(false, "-verbose").add("--release", release);
    if (Util.isModuleInfo(source.resolve("module-info.java"))) {
      javac
          .addEach(Property.TOOL_JAVAC_OPTIONS.get().lines())
          .add("-d", destination)
          .add("--module-version", project.version)
          .add("--module-path", project.modulePaths(target))
          .add("--module-source-path", realm.moduleSourcePath);
      if (base != release) {
        javac.add("--patch-module", module + '=' + baseClasses);
      }
      javac.add("--module", module);
    } else {
      javac.add("-d", destination.resolve(module));
      var classPath = new ArrayList<Path>();
      if (base != release) {
        classPath.add(baseClasses);
      }
      if (Files.isDirectory(target.modules)) {
        classPath.addAll(Util.list(target.modules, Util::isJarFile));
      }
      for (var path : Util.findExisting(project.library.modulePaths)) {
        if (Util.isJarFile(path)) {
          classPath.add(path);
          continue;
        }
        classPath.addAll(Util.list(path, Util::isJarFile));
      }
      javac.add("--class-path", classPath);
      javac.addEach(Util.find(List.of(source), Util::isJavaFile));
    }
    bach.run(javac);
  }

  private void jarModule(Project.MultiReleaseUnit unit) {
    var releases = new ArrayDeque<>(new TreeSet<>(unit.releases.keySet()));
    var base = unit.releases.get(releases.pop()).getFileName();
    var module = unit.info.descriptor().name();
    var jar =
        new Command("jar")
            .add("--create")
            .add("--file", Util.treeCreate(target.modules).resolve(target.file(unit, ".jar")))
            .addIff(bach.verbose(), "--verbose")
            .add("-C", classes.resolve(base).resolve(module))
            .add(".")
            .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add("."));
    for (var release : releases) {
      var path = unit.releases.get(release).getFileName();
      var released = classes.resolve(path).resolve(module);
      if (unit.copyModuleDescriptorToRootRelease == release) {
        jar.add("-C", released);
        jar.add("module-info.class");
      }
      jar.add("--release", release);
      jar.add("-C", released);
      jar.add(".");
    }
    bach.run(jar);
    if (bach.verbose()) {
      bach.run(new Command("jar", "--describe-module", "--file", target.modularJar(unit)));
    }
  }

  private void jarSources(Project.MultiReleaseUnit unit) {
    var releases = new ArrayDeque<>(new TreeMap<>(unit.releases).entrySet());
    var jar =
        new Command("jar")
            .add("--create")
            .add("--file", target.sourcesJar(unit))
            .addIff(bach.verbose(), "--verbose")
            .add("--no-manifest")
            .add("-C", releases.removeFirst().getValue())
            .add(".")
            .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add("."));
    for (var release : releases) {
      jar.add("--release", release.getKey());
      jar.add("-C", release.getValue());
      jar.add(".");
    }
    bach.run(jar);
  }
}
