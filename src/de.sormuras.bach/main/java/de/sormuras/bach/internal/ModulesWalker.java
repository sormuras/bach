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

import de.sormuras.bach.Project;
import de.sormuras.bach.Task;
import de.sormuras.bach.call.Jar;
import de.sormuras.bach.call.Javac;
import de.sormuras.bach.call.Javadoc;
import de.sormuras.bach.call.Jlink;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** A directory tree walker building modular project structures. */
public /*static*/ class ModulesWalker {

  /** Walk given {@link Project.Builder}'s base directory tree and replace its structure. */
  public static Project.Builder walk(Project.Builder builder) {
    var base = builder.getBase().directory();
    var moduleInfoFiles = Paths.find(List.of(base), Paths::isModuleInfoJavaFile);
    if (moduleInfoFiles.isEmpty()) throw new IllegalStateException("No module found: " + base);
    var walker = new ModulesWalker(builder, moduleInfoFiles);
    return builder.structure(walker.newStructure());
  }

  private final Project.Base base;
  private final Project.Info info;
  private final Project.Tuner tuner;
  private final List<Path> moduleInfoFiles;

  public ModulesWalker(Project.Builder builder, List<Path> moduleInfoFiles) {
    this.base = builder.getBase();
    this.info = builder.getInfo();
    this.tuner = builder.getTuner();
    this.moduleInfoFiles = moduleInfoFiles;
  }

  public Project.Structure newStructure() {
    try {
      return newStructureWithMainTestPreviewRealms();
    } catch (IllegalStateException ignore) {
      // try next
    }
    return newStructureWithSingleUnnamedRealm();
  }

  public Project.Structure newStructureWithSingleUnnamedRealm() {
    var realms = List.of(newRealm("", moduleInfoFiles, false, List.of()));
    return new Project.Structure(Project.Library.of(), realms);
  }

  public Project.Structure newStructureWithMainTestPreviewRealms() {
    var mainModuleInfoFiles = new ArrayList<Path>();
    var testModuleInfoFiles = new ArrayList<Path>();
    var viewModuleInfoFiles = new ArrayList<Path>();
    for (var moduleInfoFile : moduleInfoFiles) {
      var deque = Paths.deque(moduleInfoFile);
      if (Collections.frequency(deque, "main") == 1) {
        mainModuleInfoFiles.add(moduleInfoFile);
      } else if (Collections.frequency(deque, "test") == 1) {
        testModuleInfoFiles.add(moduleInfoFile);
      } else if (Collections.frequency(deque, "test-preview") == 1) {
        viewModuleInfoFiles.add(moduleInfoFile);
      } else {
        var message = new StringBuilder("Cannot guess realm of " + moduleInfoFile);
        message.append('\n').append('\n');
        for (var file : moduleInfoFiles) message.append("\t\t").append(file).append('\n');
        throw new IllegalStateException(message.toString());
      }
    }
    var realms = new ArrayList<Project.Realm>();
    var main = newRealm("main", mainModuleInfoFiles, false, List.of());
    if (!mainModuleInfoFiles.isEmpty()) {
      realms.add(main);
    }
    var test = newRealm("test", testModuleInfoFiles, false, List.of(main));
    if (!testModuleInfoFiles.isEmpty()) {
      realms.add(test);
    }
    var view = newRealm("test-preview", viewModuleInfoFiles, true, List.of(main, test));
    if (!viewModuleInfoFiles.isEmpty()) {
      realms.add(view);
    }
    return new Project.Structure(Project.Library.of(), realms);
  }

  public Project.Realm newRealm(
      String realm, List<Path> moduleInfoFiles, boolean preview, List<Project.Realm> upstreams) {
    var moduleNames = new TreeSet<String>();
    var moduleSourcePathPatterns = new ArrayList<String>();
    var units = new ArrayList<Project.Unit>();
    var javadocCommentFound = false;
    for (var moduleInfoFile : moduleInfoFiles) {
      javadocCommentFound = javadocCommentFound || Paths.isJavadocCommentAvailable(moduleInfoFile);
      var descriptor = Modules.describe(moduleInfoFile);
      var module = descriptor.name();
      moduleNames.add(module);
      moduleSourcePathPatterns.add(Modules.modulePatternForm(moduleInfoFile, descriptor.name()));

      var classes = base.classes(realm, module);
      var modules = base.modules(realm);
      var jar = modules.resolve(module + ".jar");

      var context = Map.of("realm", realm, "module", module);
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

      var parent = moduleInfoFile.getParent();
      units.add(new Project.Unit(descriptor, List.of(parent), List.of(task)));
    }

    var namesOfUpstreams = upstreams.stream().map(Project.Realm::name).collect(Collectors.toList());
    var patchesToUpstreams = patches(units, upstreams);

    var context = Map.of("realm", realm);
    var javac =
        new Javac()
            .setModules(moduleNames)
            .setVersionOfModulesThatAreBeingCompiled(info.version())
            .setPatternsWhereToFindSourceFiles(moduleSourcePathPatterns)
            .setPathsWhereToFindApplicationModules(base.modulePaths(namesOfUpstreams))
            .setPathsWhereToFindMoreAssetsPerModule(patchesToUpstreams)
            .setDestinationDirectory(base.classes(realm));
    if (preview) {
      javac.setCompileForVirtualMachineVersion(Runtime.version().feature());
      javac.setEnablePreviewLanguageFeatures(true);
    }
    tuner.tune(javac, context);

    var tasks = new ArrayList<Task>();
    if (javadocCommentFound) {
      var javadoc =
          new Javadoc()
              .setDestinationDirectory(base.api())
              .setModules(moduleNames)
              .setPatternsWhereToFindSourceFiles(moduleSourcePathPatterns)
              .setPathsWhereToFindApplicationModules(base.modulePaths(namesOfUpstreams))
              .setPathsWhereToFindMoreAssetsPerModule(patchesToUpstreams);
      tuner.tune(javadoc, context);
      tasks.add(javadoc.toTask());
    }

    var mainModule = Modules.findMainModule(units.stream().map(Project.Unit::descriptor));
    if (mainModule.isPresent()) {
      var jlink =
          new Jlink().setModules(moduleNames).setLocationOfTheGeneratedRuntimeImage(base.image());
      var launcher = Path.of(mainModule.get().replace('.', '/')).getFileName().toString();
      var arguments = jlink.getAdditionalArguments();
      arguments.add("--module-path", base.modules(realm));
      arguments.add("--launcher", launcher + '=' + mainModule.get());
      tuner.tune(jlink, context);
      tasks.add(
          Task.sequence(
              String.format("Create custom runtime image with '%s' as launcher", launcher),
              new Task.DeleteDirectories(base.image()),
              jlink.toTask()));
    }

    return new Project.Realm(realm, units, javac, tasks);
  }

  static Map<String, List<Path>> patches(List<Project.Unit> units, List<Project.Realm> upstreams) {
    if (units.isEmpty() || upstreams.isEmpty()) return Map.of();
    var patches = new TreeMap<String, List<Path>>();
    for (var unit : units) {
      var module = unit.name();
      for (var upstream : upstreams)
        upstream.units().stream()
            .filter(up -> up.name().equals(module))
            .findAny()
            .ifPresent(up -> patches.put(module, up.paths()));
    }
    return patches;
  }
}
