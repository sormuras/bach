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

package de.sormuras.bach.action;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Call;
import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.project.MainSources;
import de.sormuras.bach.project.SourceUnit;
import de.sormuras.bach.tool.Jar;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
import de.sormuras.bach.tool.Jlink;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * An action that compiles main sources to modules, API documentation, and a custom runtime image.
 */
public class MainRealmBuilder extends BuildRealmAction<MainSources> {

  public MainRealmBuilder(Bach bach) {
    super(bach, bach.project().sources().mainSources());
  }

  @Override
  public void buildModules() {
    buildMainModules();
    bach().run(this::buildMainApiDocumentation, this::buildMainCustomRuntimeImage);
  }

  public void buildMainModules() {
    var units = main().units();
    bach().run(computeJavacForMainSources());
    var modules = base().modules("");
    Paths.deleteDirectories(modules);
    Paths.createDirectories(modules);
    Paths.createDirectories(base().sources(""));

    var jars = new ArrayList<Jar>();
    for (var unit : units.map().values()) {
      jars.add(computeJarForMainSources(unit));
      if (!unit.sources().isMultiTarget()) {
        jars.add(computeJarForMainModule(unit));
        continue;
      }
      var module = unit.name();
      var mainClass = unit.descriptor().mainClass();
      for (var directory : unit.directories()) {
        var sourcePaths = List.of(unit.sources().first().path(), directory.path());
        var baseClasses = base().classes("", main().release().feature());
        var javac =
            Call.javac()
                .with("--release", directory.release())
                .with("--source-path", Paths.join(new TreeSet<>(sourcePaths)))
                .with("--class-path", Paths.join(List.of(baseClasses)))
                .with("-implicit:none") // generate classes for explicitly referenced source files
                .with("-d", base().classes("", directory.release(), module))
                .with(Paths.find(List.of(directory.path()), 99, Paths::isJavaFile));
        bach().run(javac);
      }
      var sources = new ArrayDeque<>(unit.directories());
      var sources0 = sources.removeFirst();
      var classes0 = base().classes("", sources0.release(), module);
      var includeSources = main().is(MainSources.Modifier.INCLUDE_SOURCES_IN_MODULAR_JAR);
      var jar =
          Call.jar()
              .with("--create")
              .withArchiveFile(project().toModuleArchive("", module))
              .with(mainClass.isPresent(), "--main-class", mainClass.orElse("?"))
              .with("-C", classes0, ".")
              .with(includeSources, "-C", sources0.path(), ".");
      var sourceDirectoryWithSolitaryModuleInfoClass = sources0;
      if (Files.notExists(classes0.resolve("module-info.class"))) {
        for (var source : sources) {
          var classes = base().classes("", source.release(), module);
          if (Files.exists(classes.resolve("module-info.class"))) {
            jar = jar.with("-C", classes, "module-info.class");
            var size = Paths.list(classes, __ -> true).size();
            if (size == 1) sourceDirectoryWithSolitaryModuleInfoClass = source;
            break;
          }
        }
      }
      for (var source : sources) {
        if (source == sourceDirectoryWithSolitaryModuleInfoClass) continue;
        var classes = base().classes("", source.release(), module);
        jar =
            jar.with("--release", source.release())
                .with("-C", classes, ".")
                .with(includeSources, "-C", source.path(), ".");
      }
      jars.add(jar);
    }
    bach().run(bach()::run, jars);
  }

  public void buildMainApiDocumentation() {
    if (main().is(MainSources.Modifier.NO_API_DOCUMENTATION)) return;
    if (!checkConditionForBuildApiDocumentation()) return;

    bach().run(computeJavadocCall());
    bach().run(computeJarForApiDocumentation());
  }

  public void buildMainCustomRuntimeImage() {
    if (main().is(MainSources.Modifier.NO_CUSTOM_RUNTIME_IMAGE)) return;
    if (!checkConditionForBuildCustomRuntimeImage()) return;

    Paths.deleteDirectories(base().workspace("image"));
    bach().run(computeJLinkForCustomRuntimeImage());
  }

  public boolean checkConditionForBuildApiDocumentation() {
    return true; // TODO Parse `module-info.java` files for Javadoc comments...
  }

  public boolean checkConditionForBuildCustomRuntimeImage() {
    var modulePaths = Paths.retainExisting(base().modules(""), base().libraries());
    var autos = Modules.findAutomaticModules(modulePaths);
    if (autos.size() > 0) {
      var message = "Creation of custom runtime image may fail -- automatic modules detected: %s";
      log(System.Logger.Level.WARNING, message, autos);
    }

    return main().findMainModule().isPresent();
  }

  public Javac computeJavacForMainSources() {
    var release = main().release().feature();
    var modulePath = Paths.joinExisting(base().libraries());
    return Call.javac()
        .withModule(main().units().toNames(","))
        .with("--module-version", project().version())
        .with(main().units().toModuleSourcePaths(false), Javac::withModuleSourcePath)
        .with(modulePath, Javac::withModulePath)
        .withEncoding("UTF-8")
        .with("-parameters")
        .withRecommendedWarnings()
        .with("-Werror")
        .with("--release", release)
        .with("-d", base().classes("", release));
  }

  public Jar computeJarForMainSources(SourceUnit unit) {
    var module = unit.name();
    var sources = new ArrayDeque<>(unit.directories());
    var file = module + '@' + project().version() + "-sources.jar";
    var jar =
        Call.jar()
            .with("--create")
            .withArchiveFile(base().sources("").resolve(file))
            .with("--no-manifest")
            .with("-C", sources.removeFirst().path(), ".");
    if (main().is(MainSources.Modifier.INCLUDE_RESOURCES_IN_SOURCES_JAR)) {
      jar = jar.with(unit.resources(), (call, resource) -> call.with("-C", resource, "."));
    }
    for (var source : sources) {
      jar = jar.with("--release", source.release());
      jar = jar.with("-C", source.path(), ".");
    }
    return jar;
  }

  public Jar computeJarForMainModule(SourceUnit unit) {
    var jar = computeJarCall(unit);
    if (main().is(MainSources.Modifier.INCLUDE_SOURCES_IN_MODULAR_JAR)) {
      jar = jar.with(unit.directories(), (call, src) -> call.with("-C", src.path(), "."));
    }
    return jar;
  }

  public Javadoc computeJavadocCall() {
    var modulePath = Paths.joinExisting(base().libraries());
    return Call.javadoc()
        .withModule(main().units().toNames(","))
        .with(main().units().toModuleSourcePaths(false), Javadoc::withModuleSourcePath)
        .with(modulePath, Javadoc::withModulePath)
        .with("-d", base().documentation("api"))
        .withEncoding("UTF-8")
        .with("-locale", "en")
        .with("-quiet")
        .with("-Xdoclint")
        .with("--show-module-contents", "all");
  }

  public Jar computeJarForApiDocumentation() {
    var file = project().name() + '@' + project().version() + "-api.jar";
    return Call.jar()
        .with("--create")
        .withArchiveFile(base().documentation(file))
        .with("--no-manifest")
        .with("-C", base().documentation("api"), ".");
  }

  public Jlink computeJLinkForCustomRuntimeImage() {
    var modulePath = Paths.joinExisting(base().modules(""), base().libraries()).orElseThrow();
    var mainModule = main().findMainModule();
    return Call.jlink()
        .with("--add-modules", main().units().toNames(","))
        .with("--module-path", modulePath)
        .with(mainModule.isPresent(), "--launcher", project().name() + '=' + mainModule.orElse("?"))
        .with("--compress", "2")
        .with("--no-header-files")
        .with("--no-man-pages")
        .with("--output", base().workspace("image"));
  }
}
