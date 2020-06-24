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

package de.sormuras.bach;

import de.sormuras.bach.internal.Concurrency;
import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.ModulesResolver;
import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.internal.Resources;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.SourceDirectory;
import de.sormuras.bach.project.SourceUnit;
import de.sormuras.bach.tool.JUnit;
import de.sormuras.bach.tool.Jar;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.TestModule;
import de.sormuras.bach.tool.Tool;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** A builder builds the project assigned to the given bach instance. */
public class Builder {

  private final Bach bach;
  private final Logbook logbook;
  private final Project project;

  public Builder(Bach bach) {
    this.bach = bach;
    this.logbook = bach.logbook();
    this.project = bach.project();
  }

  public void build() {
    var caption = "Build of " + project.toNameAndVersion();
    var projectInfoJava = String.join(System.lineSeparator(), project.toStrings());
    logbook.print(Level.INFO, "%s started...", caption);
    logbook.print(Level.DEBUG, "\tflags = %s", bach.flags());
    logbook.print(Level.DEBUG, "\tlogbook.threshold = %s", logbook.threshold());
    logbook.print(Level.TRACE, "\tproject-info.java = ...\n%s", projectInfoJava);

    var factory = Executors.defaultThreadFactory();
    var start = Instant.now();

    resolveMissingModules();

    if (project.isMainSourcePresent()) {
      try (var executor = Concurrency.shutdownOnClose(Executors.newCachedThreadPool(factory))) {
        executor.submit(this::compileMainSources);
        executor.submit(this::generateApiDocumentation);
      }
      createCustomRuntimeImage();
    }

    if (project.isTestSourcePresent()) {
      compileTestSources();
      executeTestModules();
    }

    printModuleStatistics(Level.INFO);

    var duration = Duration.between(start, Instant.now());
    logbook.print(Level.INFO, "%s took %d ms", caption, duration.toMillis());

    var markdown = logbook.toMarkdown(project);
    try {
      var logfile = project.structure().base().workspace("logbook.md");
      Files.createDirectories(logfile.getParent());
      Files.write(logfile, markdown);
      logbook.print(Level.INFO, "Logfile written to %s", logfile.toUri());
    } catch (IOException e) {
      throw new UncheckedIOException("Write logfile failed: " + e, e);
    }

    var errors = logbook.errors();
    if (errors.isEmpty()) return;

    errors.forEach(error -> error.toStrings().forEach(System.err::println));
    var message = "Detected " + errors.size() + " error" + (errors.size() != 1 ? "s" : "");
    var failOnError = bach.isFailOnError();
    logbook.print(Level.WARNING, message + " -> fail-on-error: " + failOnError);
    if (failOnError) throw new AssertionError(message);
  }

  public void resolveMissingModules() {
    var httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    var resources = new Resources(httpClient);
    var libraries = project.structure().base().libraries();
    class Transporter implements Consumer<Set<String>> {
      @Override
      public void accept(Set<String> modules) {
        for (var module : modules) {
          var raw = project.findLocator(module);
          if (raw.isEmpty()) {
            bach.logbook().print(Level.WARNING, "Module %s not locatable", module);
            continue;
          }
          try {
            var lib = Paths.createDirectories(libraries);
            var uri = URI.create(raw.get().uri());
            var name = module + ".jar";
            var file = resources.copy(uri, lib.resolve(name));
            var size = Files.size(file);
            bach.logbook().print(Level.INFO, "%s (%d bytes) << %s", file, size, uri);
          } catch (Exception e) {
            throw new Error("Resolve module '" + module + "' failed: " + raw + "\n\t" + e, e);
          }
        }
      }
    }
    var modulePaths = List.of(libraries);
    var declared = project.toDeclaredModuleNames();
    var resolver = new ModulesResolver(modulePaths, declared, new Transporter());
    resolver.resolve(project.toRequiredModuleNames());
    resolver.resolve(project.structure().requires());
  }

  public void compileMainSources() {
    var javac = project.main().javac();
    if (javac.activated()) bach.call(javac);

    var modules = project.structure().base().modules("");
    Paths.deleteDirectories(modules);
    Paths.createDirectories(modules);

    var base = project.structure().base();
    for (var unit : project.main().units().units().values()) {
      var jar = unit.jar();
      if (unit.isMultiRelease()) {
        for (var directory : unit.sources()) compile("", unit, directory);
        var module = unit.name();
        var sources = new ArrayDeque<>(unit.sources());
        var sources0 = sources.removeFirst();
        var classes0 = base.classes("", sources0.release(), module);
        jar = jar.without("-C");
        jar = jar.withChangeDirectoryAndIncludeFiles(classes0, ".");
        // if (jarModuleWithSources) arguments.add("-C", sources0.path(), ".");
        var sourceDirectoryWithSolitaryModuleInfoClass = sources0;
        if (Files.notExists(classes0.resolve("module-info.class"))) {
          for (var source : sources) {
            var classes = base.classes("", source.release(), module);
            if (Files.exists(classes.resolve("module-info.class"))) {
              jar = jar.withChangeDirectoryAndIncludeFiles(classes, "module-info.class");
              var size = Paths.list(classes, __ -> true).size();
              if (size == 1) sourceDirectoryWithSolitaryModuleInfoClass = source;
              break;
            }
          }
        }
        for (var source : sources) {
          if (source == sourceDirectoryWithSolitaryModuleInfoClass) continue;
          var classes = base.classes("", source.release(), module);
          jar = jar.with("--release", source.release());
          jar = jar.withChangeDirectoryAndIncludeFiles(classes, ".");
          // if (jarModuleWithSources) arguments.add("-C", source.path(), ".");
        }
      }
      if (jar.activated()) bach.call(jar);
    }
  }

  void compile(String realm, SourceUnit unit, SourceDirectory directory) {
    var base = project.structure().base();
    var module = unit.name();
    var sourcePaths = List.of(unit.sources().get(0).path(), directory.path());
    var baseClasses = base.classes(realm, project.basics().release().feature());
    var javac =
        Javac.of()
            .with("--release", directory.release())
            .with("-implicit:none") // generate classes for explicitly referenced source files only
            .with("-d", base.classes(realm, directory.release(), module))
            .with("--source-path", Paths.join(new TreeSet<>(sourcePaths)))
            .with("--class-path", Paths.join(List.of(baseClasses)))
            .with(Paths.find(List.of(directory.path()), 99, Paths::isJavaFile));
    bach.call(javac);
  }

  public void generateApiDocumentation() {
    var javadoc = project.main().javadoc();
    if (javadoc.activated()) {
      bach.call(javadoc);
      var destination = Path.of(javadoc.find("-d").orElseThrow());
      var base = project.structure().base();
      var file = project.basics().name() + "-" + project.basics().version() + "-api.jar";
      var jar =
          Jar.of(base.documentation(file))
              .with("--no-manifest")
              .withChangeDirectoryAndIncludeFiles(destination, ".");
      bach.call(jar);
    }
  }

  public void createCustomRuntimeImage() {
    // https://medium.com/@david.delabassee/jlink-stripping-out-native-and-java-debug-information-507e7b587dd7
    // .with("--strip-debug")
    var base = project.structure().base();
    var modulePaths = new ArrayList<Path>();
    modulePaths.add(base.modules(""));
    modulePaths.add(base.libraries());
    if (Modules.isAutomaticModulePresent(modulePaths)) return;

    var launcher = project.basics().name();
    var main = project.main();
    var mainModule = Modules.findMainModule(main.units().toUnits().map(SourceUnit::module));
    var image = base.workspace("image");
    var jlink =
        Tool.of("jlink")
            .with("--add-modules", main.units().toNames(","))
            .with("--module-path", Paths.join(modulePaths))
            .with("--output", image)
            .with(mainModule, (tool, module) -> tool.with("--launcher", launcher + '=' + module))
            .with("--compress", "2")
            .with("--no-header-files")
            .with("--no-man-pages");
    Paths.deleteDirectories(image);
    bach.call(jlink);
  }

  public void compileTestSources() {
    var test = project.test();
    var javac = test.javac();
    if (javac.activated()) bach.call(javac);

    var modules = project.structure().base().modules(test.name());
    Paths.deleteDirectories(modules);
    Paths.createDirectories(modules);

    for (var unit : test.units().units().values()) {
      var jar = unit.jar();
      if (jar.activated()) bach.call(jar);
    }
  }

  public void executeTestModules() {
    var base = project.structure().base();
    var test = project.test();
    var lib = base.libraries();
    for (var unit : test.units().units().values()) {
      var modulePaths = new ArrayList<Path>();
      modulePaths.add(Path.of(unit.jar().find("--file").orElseThrow())); // modular JAR
      modulePaths.add(base.modules("")); // main modules
      modulePaths.add(base.modules("test")); // other test modules
      if (Files.exists(lib)) modulePaths.add(lib); // external modules

      var module = unit.name();
      var testModule = new TestModule(module, modulePaths);
      if (testModule.tool().isPresent()) bach.call(testModule);

      var junit =
          new JUnit(module, modulePaths, List.of())
              .with("--select-module", module)
              .with("--disable-ansi-colors")
              .with("--reports-dir", base.reports("junit", module));
      if (junit.tool().isPresent()) bach.call(junit);
    }
  }

  public void printModuleStatistics(Level level) {
    var directory = project.structure().base().modules("");
    if (Files.notExists(directory)) return;
    var uri = directory.toUri().toString();
    var files = Paths.list(directory, Paths::isJarFile);
    logbook.print(level, "Directory %s contains", uri);
    if (files.isEmpty()) logbook.print(Level.WARNING, "Not a single JAR file?!");
    for (var file : files) logbook.print(level, "%,12d %s", Paths.size(file), file.getFileName());
  }
}
