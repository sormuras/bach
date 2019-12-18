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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/*BODY*/
/** Multi-release module compiler. */
public /*STATIC*/ class Hydra {

  private final Bach bach;
  private final Realm realm;
  private final Folder folder;
  private final Path classes;

  public Hydra(Bach bach, Realm realm) {
    this.bach = bach;
    this.realm = realm;
    this.folder = bach.getProject().folder();
    this.classes = folder.realm(realm.name()).resolve("classes/hydra");
  }

  public void compile(List<Unit> units) {
    for (var unit : units) {
      if (unit.isMultiRelease()) {
        compile(unit);
      }
    }
  }

  private void compile(Unit unit) {
    var base = unit.sources().get(0);
    bach.getLog().debug("Base feature release number is: %d", base.release());

    for (var source : unit.sources()) {
      compile(unit, base, source);
    }
    jarModule(unit);
    jarSources(unit);
    // javadoc(unit) and jarJavadoc(unit) is provided by Jigsaw
  }

  private void compile(Unit unit, Source base, Source source) {
    var project = bach.getProject();
    var module = unit.name();
    var baseClasses = classes.resolve(base.path().getFileName()).resolve(module);
    var destination = classes.resolve(source.path().getFileName());
    var modules = folder.modules(realm.name());
    var javac = new Call("javac").add("--release", source.release());
    if (Files.isRegularFile(source.path().resolve("module-info.java"))) {
      var modulePath = new ArrayList<Path>();
      modulePath.add(modules); // current realm first, like "main/modules"...
      modulePath.addAll(realm.modulePaths()); // dependencies last, like "lib"...
      javac
          .forEach(realm.argumentsFor("javac"), Call::add)
          .add("--class-path", "")
          .add("-d", destination.resolve(module))
          .add("--module-version", project.version(unit))
          .add("--module-path", Paths.filterExisting(modulePath));
      //          .add("--module-source-path", source.path().toString().replace(module, "*"));
      if (base != source) {
        javac.add("--patch-module", module + '=' + baseClasses);
      }
      //      javac.add("--module", module);
    } else {
      javac.forEach(realm.argumentsFor("javac"), Call::add).add("-d", destination.resolve(module));
      var classPath = new ArrayList<Path>();
      if (base != source) {
        classPath.add(baseClasses);
      }
      if (Files.isDirectory(modules)) {
        classPath.addAll(Paths.list(modules, Paths::isJarFile));
      }
      for (var path : Paths.filterExisting(realm.modulePaths())) {
        if (Paths.isJarFile(path)) {
          classPath.add(path);
          continue;
        }
        classPath.addAll(Paths.list(path, Paths::isJarFile));
        var jarsRelease = path.resolve("java-" + source.release());
        if (Files.isDirectory(jarsRelease)) {
          classPath.addAll(Paths.list(jarsRelease, Paths::isJarFile));
        }
      }
      javac.add("--class-path", classPath);
    }
    javac.forEach(Paths.find(List.of(source.path()), Paths::isJavaFile), Call::add);
    bach.execute(javac);
  }

  private void jarModule(Unit unit) {
    var sources = new ArrayDeque<>(unit.sources());
    var base = sources.pop().path().getFileName();
    var module = unit.name();
    var file = bach.getProject().modularJar(unit);
    var root = classes.resolve(base).resolve(module);
    var jar =
        new Call("jar")
            .add("--create")
            .add("--file", file)
            .iff(bach.isVerbose(), c -> c.add("--verbose"))
            .add("-C", root)
            .add(".")
            .forEach(unit.resources(), (cmd, path) -> cmd.add("-C", path).add("."));
    var roots = new ArrayList<Path>();
    roots.add(root);
    for (var source : sources) {
      var path = source.path().getFileName();
      var released = classes.resolve(path).resolve(module);
      roots.add(released);
      if (source.isVersioned()) {
        jar.add("--release", source.release());
      }
      jar.add("-C", released);
      jar.add(".");
    }
    Paths.createDirectories(folder.modules(unit.realm().name()));
    bach.execute(jar);
    // Ensure root of MR-JAR contains a module descriptor.
    roots.removeIf(path -> Files.notExists(path.resolve("module-info.class")));
    if (roots.isEmpty()) throw new AssertionError("Not a single module-info.class available?");
    if (Files.notExists(root.resolve("module-info.class"))) {
      bach.getLog().debug("No root module descriptor found: %s", root);
      bach.execute("jar", "--update", "--file", file, "-C", roots.get(0), "module-info.class");
    }
    if (bach.isVerbose()) {
      bach.execute("jar", "--describe-module", "--file", file);
    }
  }

  private void jarSources(Unit unit) {
    var file = bach.getProject().sourcesJar(unit); // "../{REALM}/{MODULE}-{VERSION}-sources.jar"
    var sources = new ArrayDeque<>(unit.sources());
    var jar =
        new Call("jar")
            .add("--create")
            .add("--file", file)
            .iff(bach.isVerbose(), c -> c.add("--verbose"))
            .add("--no-manifest")
            .add("-C", sources.removeFirst().path())
            .add(".")
            .forEach(unit.resources(), (cmd, path) -> cmd.add("-C", path).add("."));
    for (var source : sources) {
      jar.add("--release", source.release());
      jar.add("-C", source.path());
      jar.add(".");
    }
    bach.execute(jar);
  }
}
