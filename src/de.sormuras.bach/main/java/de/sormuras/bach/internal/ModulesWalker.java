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

package de.sormuras.bach.internal;

import de.sormuras.bach.Call;
import de.sormuras.bach.Project;
import de.sormuras.bach.Task;
import de.sormuras.bach.call.Jar;
import de.sormuras.bach.call.Javac;
import de.sormuras.bach.call.Javadoc;
import de.sormuras.bach.call.Jlink;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Walk a directory tree, find modules, organize a structure and return a {@link Project.Builder}.
 */
public /*static*/ class ModulesWalker {

  public static Project.Builder walk(Path directory) {
    return walk(Project.Base.of(directory), Project::tuner);
  }

  public static Project.Builder walk(Project.Base base, Call.Tuner tuner) {
    var moduleInfoFiles = Paths.find(List.of(base.directory()), Paths::isModuleInfoJavaFile);
    if (moduleInfoFiles.isEmpty()) throw new IllegalStateException("No module found: " + base);
    var walker = new ModulesWalker(base, moduleInfoFiles, tuner);
    return walker.walkAndCreateUnnamedRealm();
  }

  private final Project.Base base;
  private final List<Path> moduleInfoFiles;
  private final Call.Tuner tuner;

  public ModulesWalker(Project.Base base, List<Path> moduleInfoFiles, Call.Tuner tuner) {
    this.base = base;
    this.moduleInfoFiles = moduleInfoFiles;
    this.tuner = tuner;
  }

  public Project.Builder walkAndCreateUnnamedRealm() {
    var moduleNames = new TreeSet<String>();
    var moduleSourcePathPatterns = new TreeSet<String>();
    var units = new ArrayList<Project.Unit>();
    var javadocCommentFound = false;
    for (var info : moduleInfoFiles) {
      javadocCommentFound = javadocCommentFound || Paths.isJavadocCommentAvailable(info);
      var descriptor = Modules.describe(info);
      var module = descriptor.name();
      moduleNames.add(module);
      moduleSourcePathPatterns.add(Modules.modulePatternForm(info, descriptor.name()));

      var classes = base.classes("", module);
      var modules = base.modules("");
      var jar = modules.resolve(module + ".jar");

      var context = new Call.Context("", module);
      var jarCreate = new Jar();
      var jarCreateArgs = jarCreate.getAdditionalArguments();
      jarCreateArgs.add("--create").add("--file", jar);
      descriptor.mainClass().ifPresent(main -> jarCreateArgs.add("--main-class", main));
      jarCreateArgs.add("-C", classes, ".");
      tuner.tune(jarCreate, context);
      var jarDescribe = new Jar();
      jarDescribe.getAdditionalArguments().add("--describe-module").add("--file", jar);
      tuner.tune(jarDescribe, context);
      var task =
          Task.sequence(
              "Create modular JAR file " + jar.getFileName(),
              new Task.CreateDirectories(jar.getParent()),
              jarCreate.toTask(),
              jarDescribe.toTask());

      units.add(new Project.Unit(descriptor, List.of(task)));
    }

    var context = new Call.Context("", null);
    var javac =
        new Javac()
            .setModules(moduleNames)
            .setPatternsWhereToFindSourceFiles(moduleSourcePathPatterns)
            .setDestinationDirectory(base.classes(""));
    tuner.tune(javac, context);

    var tasks = new ArrayList<Task>();
    if (javadocCommentFound) {
      var javadoc =
          new Javadoc()
              .setDestinationDirectory(base.api())
              .setModules(moduleNames)
              .setPatternsWhereToFindSourceFiles(moduleSourcePathPatterns);
      tuner.tune(javadoc, context);
      tasks.add(javadoc.toTask());
    }

    var mainModule = Modules.findMainModule(units.stream().map(Project.Unit::descriptor));
    if (mainModule.isPresent()) {
      var jlink =
          new Jlink().setModules(moduleNames).setLocationOfTheGeneratedRuntimeImage(base.image());
      var launcher = Path.of(mainModule.get().replace('.', '/')).getFileName().toString();
      var arguments = jlink.getAdditionalArguments();
      arguments.add("--module-path", base.modules(""));
      arguments.add("--launcher", launcher + '=' + mainModule.get());
      tuner.tune(jlink, context);
      tasks.add(
          Task.sequence(
              String.format("Create custom runtime image with '%s' as launcher", launcher),
              new Task.DeleteDirectories(base.image()),
              jlink.toTask()));
    }

    var realm = new Project.Realm("", units, javac.toTask(), tasks);
    var directoryName = base.directory().toAbsolutePath().getFileName();

    return new Project.Builder()
        .base(base)
        .title("Project " + Optional.ofNullable(directoryName).map(Path::toString).orElse("?"))
        .structure(new Project.Structure(Project.Library.of(), List.of(realm)));
  }
}
