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
      compile(realm.unit(module).orElseThrow());
    }
  }

  private void compile(Project.ModuleUnit unit) {
    var base = unit.sources.get(0);
    bach.log("Base feature release number is: %d", base.release);

    for (var source : unit.sources) {
      compile(unit, base, source);
    }
    jarModule(unit);
    jarSources(unit);
  }

  private void compile(Project.ModuleUnit unit, Project.Source base, Project.Source source) {
    var module = unit.info.descriptor().name();
    var baseClasses = classes.resolve(base.path.getFileName()).resolve(module);
    var destination = classes.resolve(source.path.getFileName());
    var javac = new Command("javac").add("--release", source.release);
    if (Util.isModuleInfo(source.path.resolve("module-info.java"))) {
      javac
          .addEach(realm.toolArguments.javac)
          .add("-d", destination)
          .add("--module-version", project.version)
          .add("--module-path", project.modulePaths(target))
          .add("--module-source-path", realm.moduleSourcePath);
      if (base != source) {
        javac.add("--patch-module", module + '=' + baseClasses);
      }
      javac.add("--module", module);
    } else {
      javac.add("-d", destination.resolve(module));
      var classPath = new ArrayList<Path>();
      if (base != source) {
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
      javac.addEach(Util.find(List.of(source.path), Util::isJavaFile));
    }
    bach.run(javac);
  }

  private void jarModule(Project.ModuleUnit unit) {
    var sources = new ArrayDeque<>(unit.sources);
    var base = sources.pop().path.getFileName();
    var module = unit.info.descriptor().name();
    var jar =
        new Command("jar")
            .add("--create")
            .add("--file", Util.treeCreate(target.modules).resolve(target.file(unit, ".jar")))
            .addIff(bach.verbose(), "--verbose")
            .add("-C", classes.resolve(base).resolve(module))
            .add(".")
            .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add("."));
    for (var source : sources) {
      var path = source.path.getFileName();
      var released = classes.resolve(path).resolve(module);
      if (source.merge) {
        jar.add("-C", released);
        jar.add("module-info.class");
      }
      jar.add("--release", source.release);
      jar.add("-C", released);
      jar.add(".");
    }
    bach.run(jar);
    if (bach.verbose()) {
      bach.run(new Command("jar", "--describe-module", "--file", target.modularJar(unit)));
    }
  }

  private void jarSources(Project.ModuleUnit unit) {
    var sources = new ArrayDeque<>(unit.sources);
    var jar =
        new Command("jar")
            .add("--create")
            .add("--file", target.sourcesJar(unit))
            .addIff(bach.verbose(), "--verbose")
            .add("--no-manifest")
            .add("-C", sources.removeFirst().path)
            .add(".")
            .addEach(unit.resources, (cmd, path) -> cmd.add("-C", path).add("."));
    for (var source : sources) {
      jar.add("--release", source.release);
      jar.add("-C", source.path);
      jar.add(".");
    }
    bach.run(jar);
  }
}
