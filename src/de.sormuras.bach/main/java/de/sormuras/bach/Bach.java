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

import de.sormuras.bach.internal.Factory;
import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.internal.Resolver;
import de.sormuras.bach.internal.Resources;
import de.sormuras.bach.internal.SormurasModulesProperties;
import de.sormuras.bach.project.Base;
import de.sormuras.bach.project.Link;
import de.sormuras.bach.project.MainSources;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.SourceUnit;
import de.sormuras.bach.tool.JUnit;
import de.sormuras.bach.tool.Jar;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
import de.sormuras.bach.tool.Jlink;
import de.sormuras.bach.tool.TestModule;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

/** Bach - Java Shell Builder - An extensible build workflow. */
public class Bach {

  /** Version of the Java Shell Builder. */
  public static final Version VERSION = Version.parse("11-ea");

  /**
   * Main entry-point.
   *
   * @param args the arguments
   */
  public static void main(String... args) {
    Main.main(args);
  }

  @Factory
  public static Bach of(Project project) {
    var configuration = Configuration.ofSystem();
    return new Bach(configuration, project);
  }

  @Factory
  public static Bach of(UnaryOperator<Project> operator) {
    var project = Project.of(Base.of());
    return of(operator.apply(project));
  }

  private final Configuration configuration;
  private final Project project;
  private /*lazy*/ HttpClient http;

  public Bach(Configuration configuration, Project project) {
    this.configuration = configuration;
    this.project = project;
  }

  public final Configuration configuration() {
    return configuration;
  }

  public final Configuration.Flags flags() {
    return configuration.flags();
  }

  public final Logbook logbook() {
    return configuration.logbook();
  }

  public final String log(Level level, String text) {
    return logbook().log(level, text);
  }

  public final String log(Level level, String format, Object... args) {
    return logbook().log(level, format, args);
  }

  public final Project project() {
    return project;
  }

  public final Base base() {
    return project.base();
  }

  public final MainSources main() {
    return project.sources().mainSources();
  }

  public final HttpClient http() {
    if (http == null) http = computeHttpClient();
    return http;
  }

  //
  // ===
  //

  void executeCall(Call<?> call) {
    var failFast = flags().isFailFast();

    log(Level.INFO, call.toDescriptiveLine());
    log(Level.DEBUG, call.toCommandLine());

    var provider = call.findProvider();
    if (provider.isEmpty()) {
      var message = log(Level.ERROR, "Tool provider with name '%s' not found", call.name());
      if (failFast) throw new AssertionError(message);
      return;
    }

    if (flags().isDryRun()) return;

    var tool = provider.get();
    var currentThread = Thread.currentThread();
    var currentContextLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(tool.getClass().getClassLoader());
    var out = new StringWriter();
    var err = new StringWriter();
    var args = call.toStringArray();
    var start = Instant.now();

    try {
      var code = tool.run(new PrintWriter(out), new PrintWriter(err), args);

      var duration = Duration.between(start, Instant.now());
      var normal = out.toString().strip();
      var errors = err.toString().strip();
      var result = logbook().print(call, normal, errors, duration, code);
      log(Level.DEBUG, "%s finished after %d ms", tool.name(), duration.toMillis());

      if (code == 0) return;

      var caption = log(Level.ERROR, "%s failed with exit code %d", tool.name(), code);
      var message = new StringJoiner(System.lineSeparator());
      message.add(caption);
      result.toStrings().forEach(message::add);
      if (failFast) throw new AssertionError(message);
    } catch (RuntimeException exception) {
      log(Level.ERROR, "%s failed throwing %s", tool.name(), exception);
      if (failFast) throw exception;
    } finally {
      currentThread.setContextClassLoader(currentContextLoader);
    }
  }

  void printStatistics(Level level, Path directory) {
    var uri = directory.toUri().toString();
    var files = Paths.list(directory, Paths::isJarFile);
    log(level, "Directory %s contains", uri);
    if (files.isEmpty()) log(Level.WARNING, "Not a single JAR file?!");
    for (var file : files) log(level, "%,12d %s", Paths.size(file), file.getFileName());
  }

  void writeLogbook() {
    try {
      Paths.createDirectories(base().workspace());
      var path = base().workspace("logbook.md");
      Files.write(path, logbook().toMarkdown(project()));
      log(Level.INFO, "Wrote logbook to %s", path.toUri());
    } catch (Exception exception) {
      var message = log(Level.ERROR, "write logbook failed: %s", exception);
      if (flags().isFailOnError()) throw new AssertionError(message, exception);
    }
  }

  //
  // ===
  //

  public void build() {
    log(Level.TRACE, toString());
    log(Level.TRACE, "\tflags.set=%s", flags().set());
    log(Level.TRACE, "\tlogbook.threshold=%s", logbook().threshold());
    buildProject();
  }

  public void buildProject() {
    log(Level.INFO, "Build of project %s started", project().toNameAndVersion());
    log(Level.TRACE, "project-info.java\n" + String.join("\n", project().toStrings()));
    try {
      var start = Instant.now();
      buildLibrariesDirectoryByResolvingMissingExternalModules();
      buildProjectModules();
      var duration = Duration.between(start, Instant.now()).toMillis();
      if (main().units().isPresent()) {
        printStatistics(Level.INFO, base().modules(""));
      }
      log(Level.INFO, "Build of project %s took %d ms", project().toNameAndVersion(), duration);
    } catch (Exception exception) {
      var message = log(Level.ERROR, "build failed throwing %s", exception);
      if (flags().isFailOnError()) throw new AssertionError(message, exception);
    } finally {
      writeLogbook();
    }
    var errors = logbook().errors();
    if (errors.isEmpty()) return;
    errors.forEach(error -> error.toStrings().forEach(System.err::println));
    var message = "Detected " + errors.size() + " error" + (errors.size() != 1 ? "s" : "");
    if (flags().isFailOnError()) throw new AssertionError(message);
  }

  void buildProjectModules() {
    if (main().units().isPresent()) {
      buildMainModules();
      var service = Executors.newWorkStealingPool();
      service.execute(this::buildApiDocumentation);
      service.execute(this::buildCustomRuntimeImage);
      service.shutdown();
      try {
        service.awaitTermination(1, TimeUnit.DAYS);
      } catch (InterruptedException e) {
        Thread.interrupted();
        return;
      }
    }

    if (project().sources().testSources().units().isPresent()) {
      buildTestModules();
      printStatistics(Level.DEBUG, base().modules("test"));
      buildTestReportsByExecutingTestModules();
    }
  }

  public void buildLibrariesDirectoryByResolvingMissingExternalModules() {
    // get external requires from all module-info.java files
    // get external modules from project descriptor
    // download them
    // get missing external modules from libraries directory
    // download them recursively

    var resolver =
        new Resolver(
            List.of(base().libraries()),
            project().toDeclaredModuleNames(),
            this::buildLibrariesDirectoryByResolvingModules);
    resolver.resolve(project().toRequiredModuleNames()); // from all module-info.java files
    resolver.resolve(project().library().requires()); // from project descriptor
  }

  public void buildLibrariesDirectoryByResolvingModules(Set<String> modules) {
    log(Level.INFO, "Resolve %d missing external module(s) << %s", modules.size(), modules);
    var resources = new Resources(http());
    for (var module : modules) {
      var optionalLink =
          project().library().findLink(module).or(() -> computeLinkForUnlinkedModule(module));
      if (optionalLink.isEmpty()) {
        log(Level.WARNING, "Module %s not locatable", module);
        continue;
      }
      var link = optionalLink.orElseThrow();
      var uri = link.toURI();
      var name = module + ".jar";
      try {
        var lib = Paths.createDirectories(base().libraries());
        var file = resources.copy(uri, lib.resolve(name));
        var size = Paths.size(file);
        log(Level.INFO, "%,12d %-42s << %s", size, file, uri);
      } catch (Exception e) {
        throw new Error("Resolve module '" + module + "' failed: " + uri + "\n\t" + e, e);
      }
    }
  }

  public void buildMainModules() {
    var units = main().units();
    log(Level.DEBUG, "Build of %d main module(s) started", units.size());
    executeCall(computeJavacForMainSources());
    var modules = base().modules("");
    Paths.deleteDirectories(modules);
    Paths.createDirectories(modules);
    Paths.createDirectories(base().sources(""));

    var includeSources = flags().isIncludeSourcesInModularJar();
    for (var unit : units.map().values()) {
      executeCall(computeJarForMainSources(unit));
      if (!unit.sources().isMultiTarget()) {
        executeCall(computeJarForMainModule(unit));
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
        executeCall(javac);
      }
      var sources = new ArrayDeque<>(unit.directories());
      var sources0 = sources.removeFirst();
      var classes0 = base().classes("", sources0.release(), module);
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
      executeCall(jar);
    }
  }

  public void buildApiDocumentation() {
    executeCall(computeJavadocForMainSources());
    executeCall(computeJarForApiDocumentation());
  }

  public void buildCustomRuntimeImage() {
    var modulePaths = Paths.retainExisting(base().modules(""), base().libraries());
    var autos = Modules.findAutomaticModules(modulePaths);
    if (autos.size() > 0) {
      log(Level.WARNING, "Automatic module(s) detected: %s", autos);
      return;
    }
    Paths.deleteDirectories(base().workspace("image"));
    var jlink = computeJLinkForCustomRuntimeImage();
    executeCall(jlink);
  }

  public void buildTestModules() {
    var units = project().sources().testSources().units();
    log(Level.DEBUG, "Build of %d test module(s) started", units.size());
    executeCall(computeJavacForTestSources());
    Paths.createDirectories(base().modules("test"));
    units.toUnits().map(this::computeJarForTestModule).forEach(this::executeCall);
  }

  public void buildTestReportsByExecutingTestModules() {
    var test = project().sources().testSources();
    for (var unit : test.units().map().values())
      buildTestReportsByExecutingTestModule("test", unit);
  }

  public void buildTestReportsByExecutingTestModule(String realm, SourceUnit unit) {
    var module = unit.name();
    var modulePaths =
        Paths.retainExisting(
            project().toModuleArchive(realm, module), // test module
            base().modules(""), // main modules
            base().modules(realm), // other test modules
            base().libraries()); // external modules
    log(Level.DEBUG, "Run tests in '%s' with module-path: %s", module, modulePaths);

    var testModule = new TestModule(module, modulePaths);
    if (testModule.findProvider().isPresent()) executeCall(testModule);

    var junit = computeJUnitCall(realm, unit, modulePaths);
    if (junit.findProvider().isPresent()) executeCall(junit);
  }

  public HttpClient computeHttpClient() {
    return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
  }

  private SormurasModulesProperties sormurasModulesProperties = null;

  public Optional<Link> computeLinkForUnlinkedModule(String module) {
    if (sormurasModulesProperties == null) {
      sormurasModulesProperties = new SormurasModulesProperties(this::http, Map.of());
    }
    return sormurasModulesProperties.lookup(module);
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
    if (flags().isIncludeResourcesInSourcesJar()) {
      jar = jar.with(unit.resources(), (call, resource) -> call.with("-C", resource, "."));
    }
    for (var source : sources) {
      jar = jar.with("--release", source.release());
      jar = jar.with("-C", source.path(), ".");
    }
    return jar;
  }

  public Jar computeJarForMainModule(SourceUnit unit) {
    var module = unit.name();
    var release = main().release().feature();
    var classes = base().classes("", release, module);
    var mainClass = unit.descriptor().mainClass();
    var resources = unit.resources();
    var jar =
        Call.jar()
            .with("--create")
            .withArchiveFile(project().toModuleArchive("", module))
            .with(mainClass.isPresent(), "--main-class", mainClass.orElse("?"))
            .with("-C", classes, ".")
            .with(resources, (call, resource) -> call.with("-C", resource, "."));
    if (flags().isIncludeSourcesInModularJar()) {
      jar = jar.with(unit.directories(), (call, src) -> call.with("-C", src.path(), "."));
    }
    return jar;
  }

  public Javadoc computeJavadocForMainSources() {
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
    var mainModule = Modules.findMainModule(main().units().toUnits().map(SourceUnit::descriptor));
    return Call.jlink()
        .with("--add-modules", main().units().toNames(","))
        .with("--module-path", modulePath)
        .with(mainModule.isPresent(), "--launcher", project().name() + '=' + mainModule.orElse("?"))
        .with("--compress", "2")
        .with("--no-header-files")
        .with("--no-man-pages")
        .with("--output", base().workspace("image"));
  }

  public Javac computeJavacForTestSources() {
    var release = Runtime.version().feature();
    var sources = project().sources();
    var units = sources.testSources().units();
    var modulePath = Paths.joinExisting(base().modules(""), base().libraries());
    return Call.javac()
        .withModule(units.toNames(","))
        .with("--module-version", project().version().toString() + "-test")
        .with(units.toModuleSourcePaths(false), Javac::withModuleSourcePath)
        .with(
            units.toModulePatches(main().units()).entrySet(),
            (javac, patch) -> javac.withPatchModule(patch.getKey(), patch.getValue()))
        .with(modulePath, Javac::withModulePath)
        .withEncoding("UTF-8")
        .with("-parameters")
        .withRecommendedWarnings()
        .with("-d", base().classes("test", release));
  }

  public Jar computeJarForTestModule(SourceUnit unit) {
    var module = unit.name();
    var release = Runtime.version().feature();
    var classes = base().classes("test", release, module);
    var resources = new ArrayList<>(unit.resources()); // TODO Include main resources if patched
    return Call.jar()
        .with("--create")
        .withArchiveFile(project().toModuleArchive("test", module))
        .with("-C", classes, ".")
        .with(resources, (call, resource) -> call.with("-C", resource, "."));
  }

  public JUnit computeJUnitCall(String realm, SourceUnit unit, List<Path> modulePaths) {
    var module = unit.name();
    return new JUnit(module, modulePaths, List.of())
        .with("--select-module", module)
        .with("--disable-ansi-colors")
        .with("--reports-dir", base().reports("junit-" + realm, module));
  }

  @Override
  public String toString() {
    return "Bach.java " + VERSION;
  }
}
