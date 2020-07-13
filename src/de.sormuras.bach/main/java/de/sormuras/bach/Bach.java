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
import de.sormuras.bach.project.Realm;
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

/**
 * Java Shell Builder - build modular projects with JDK tools.
 *
 * <p>As an example, a project named {@code demo} and with version {@code 47.11} can be built with
 * the following code:
 *
 * <pre>{@code
 * var configuration = Configuration.ofSystem();
 * var project = Project.of("demo", "47.11");
 * new Bach(configuration, project).build();
 * }</pre>
 *
 * <p>The Java® Development Kit provides at least the following tools via the {@link
 * java.util.spi.ToolProvider ToolProvider} interface.
 *
 * <ul>
 *   <li>{@code jar} - create an archive for classes and resources, and manipulate or restore
 *       individual classes or resources from an archive
 *   <li>{@code javac} - read Java class and interface definitions and compile them into bytecode
 *       and class files
 *   <li>{@code javadoc} - generate HTML pages of API documentation from Java source files
 *   <li>{@code javap} - disassemble one or more class files
 *   <li>{@code jdeps} - launch the Java class dependency analyzer
 *   <li>{@code jlink} - assemble and optimize a set of modules and their dependencies into a custom
 *       runtime image
 *   <li>{@code jmod} - create JMOD files and list the content of existing JMOD files
 * </ul>
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/14/docs/specs/man">JDK Tools</a>
 */
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

  /**
   * Create new Bach instance for the given project.
   *
   * @param project The project instance to use
   * @return A new {@link Bach} instance
   */
  @Factory
  public static Bach of(Project project) {
    var configuration = Configuration.ofSystem();
    return new Bach(configuration, project);
  }

  /**
   * Create new Bach instance with a project parsed from current user directory.
   *
   * @param operator The operator may return a modified project based on the parsed one
   * @see UnaryOperator#identity()
   * @return A new {@link Bach} instance
   */
  @Factory
  public static Bach of(UnaryOperator<Project> operator) {
    var project = Project.ofCurrentDirectory();
    return of(operator.apply(project));
  }

  private final Configuration configuration;
  private final Project project;
  private /*lazy*/ HttpClient http = null;
  private /*lazy*/ SormurasModulesProperties sormurasModulesProperties = null;

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

  public final boolean is(Flag flag) {
    return flags().set().contains(flag);
  }

  public final boolean not(Flag flag) {
    return !is(flag);
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

  public void build() {
    log(Level.TRACE, toString());
    log(Level.TRACE, "\tflags.set=%s", flags().set());
    log(Level.TRACE, "\tlogbook.threshold=%s", logbook().threshold());
    buildProject();
  }

  public void buildProject() {
    log(Level.INFO, "Build of project %s started by %s", project().toNameAndVersion(), this);
    log(Level.TRACE, "project-info.java\n" + String.join("\n", project().toStrings()));
    try {
      var start = Instant.now();
      buildLibrariesDirectoryByResolvingMissingExternalModules();
      buildProjectModules();
      var duration = Duration.between(start, Instant.now()).toMillis();
      logbook().print("");
      log(Level.INFO, "Build of project %s took %d ms", project().toNameAndVersion(), duration);
    } catch (Exception exception) {
      var message = log(Level.ERROR, "build failed throwing %s", exception);
      if (is(Flag.FAIL_ON_ERROR)) throw new AssertionError(message, exception);
    } finally {
      logbook().write(this);
    }

    logbook().printSummaryAndCheckErrors(this, System.err::println);
  }

  public void buildProjectModules() {
    if (main().units().isPresent()) {
      logbook().print("// <main>");
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
      logbook().print("// test");
      buildTestModules();
      buildTestReportsByExecutingTestModules();
    }

    if (project().sources().testPreview().units().isPresent()) {
      logbook().print("// test-preview");
      buildTestPreviewModules();
      buildTestReportsByExecutingTestPreviewModules();
    }
  }

  public void buildLibrariesDirectoryByResolvingMissingExternalModules() {
    // get external requires from all module-info.java files
    // get external modules from project descriptor
    // download them
    // get missing external modules from libraries directory
    // download them recursively
    logbook().print("// resolve missing external modules");
    var resolver =
        new Resolver(
            List.of(base().libraries()),
            project().toDeclaredModuleNames(),
            this::buildLibrariesDirectoryByResolvingModules);
    resolver.resolve(project().toRequiredModuleNames()); // from all module-info.java files
    resolver.resolve(project().library().requires()); // from project descriptor
  }

  public void buildLibrariesDirectoryByResolvingModules(Set<String> modules) {
    var listing = String.join(", ", modules);
    if (modules.size() == 1) log(Level.INFO, "Resolve missing external module %s", listing);
    else log(Level.INFO, "Resolve %d missing external modules: %s", modules.size(), listing);

    var resources = new Resources(http());
    for (var module : modules) {
      var details = "";
      var optionalLink = project().library().findLink(module);
      if (optionalLink.isEmpty()) {
        log(Level.WARNING, "Module %s not linked in project's library", module);
        details = " // computed link - consider creating a link in project's library";
        optionalLink = computeLinkForUnlinkedModule(module);
      }
      if (optionalLink.isEmpty()) {
        log(Level.ERROR, "Module %s not resolvable", module);
        continue;
      }
      var link = optionalLink.orElseThrow();
      var uri = link.toURI();
      log(Level.INFO, "- %s%s", module, details);
      log(Level.INFO, "\t<< %s", uri);
      try {
        var lib = Paths.createDirectories(base().libraries());
        var file = resources.copy(uri, lib.resolve(link.toModularJarFileName()));
        var size = Paths.size(file);
        log(Level.INFO, "\t>> %s with %,d bytes", file, size);
      } catch (Exception e) {
        throw new Error("Resolve module '" + module + "' failed: " + uri + "\n\t" + e, e);
      }
    }
  }

  public void buildMainModules() {
    var units = main().units();
    log(Level.DEBUG, "Build of %d main module(s) started", units.size());
    call(computeJavacForMainSources());
    var modules = base().modules("");
    Paths.deleteDirectories(modules);
    Paths.createDirectories(modules);
    Paths.createDirectories(base().sources(""));

    for (var unit : units.map().values()) {
      call(computeJarForMainSources(unit));
      if (!unit.sources().isMultiTarget()) {
        call(computeJarForMainModule(unit));
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
        call(javac);
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
              .with(is(Flag.INCLUDE_SOURCES_IN_MODULAR_JAR), "-C", sources0.path(), ".");
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
                .with(is(Flag.INCLUDE_SOURCES_IN_MODULAR_JAR), "-C", source.path(), ".");
      }
      call(jar);
    }
  }

  public void buildApiDocumentation() {
    call(computeJavadocForMainSources());
    call(computeJarForApiDocumentation());
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
    call(jlink);
  }

  public void buildTestModules() {
    var units = project().sources().testSources().units();
    log(Level.DEBUG, "Build of %d test module(s) started", units.size());
    call(computeJavacForTestSources());
    Paths.createDirectories(base().modules("test"));
    units.toUnits().map(this::computeJarForTestModule).forEach(this::call);
  }

  public void buildTestPreviewModules() {
    var units = project().sources().testPreview().units();
    log(Level.DEBUG, "Build of %d test-preview module(s) started", units.size());
    call(computeJavacForTestPreview());
    Paths.createDirectories(base().modules("test-preview"));
    units.toUnits().map(this::computeJarForTestPreviewModule).forEach(this::call);
  }

  public void buildTestReportsByExecutingTestModules() {
    var test = project().sources().testSources();
    for (var unit : test.units().map().values())
      buildTestReportsByExecutingTestModule("test", unit);
  }

  public void buildTestReportsByExecutingTestPreviewModules() {
    var preview = project().sources().testPreview();
    for (var unit : preview.units().map().values())
      buildTestReportsByExecutingTestPreviewModule("test-preview", unit);
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
    if (testModule.findProvider().isPresent()) call(testModule);

    var junit = computeJUnitCall(realm, unit, modulePaths);
    if (junit.findProvider().isPresent()) call(junit);
  }

  public void buildTestReportsByExecutingTestPreviewModule(String realm, SourceUnit unit) {
    var module = unit.name();
    var modulePaths =
        Paths.retainExisting(
            project().toModuleArchive(realm, module), // test module
            base().modules(""), // main modules
            base().modules("test"), // test modules
            base().modules(realm), // other test-preview modules
            base().libraries()); // external modules
    log(Level.DEBUG, "Run tests in '%s' with module-path: %s", module, modulePaths);

    var testModule = new TestModule(module, modulePaths);
    if (testModule.findProvider().isPresent()) call(testModule);

    var junit = computeJUnitCall(realm, unit, modulePaths);
    if (junit.findProvider().isPresent()) call(junit);
  }

  void call(Call<?> call) {
    log(Level.INFO, call.toDescriptiveLine());
    log(Level.DEBUG, call.toCommandLine());

    var provider = call.findProvider();
    if (provider.isEmpty()) {
      var message = log(Level.ERROR, "Tool provider with name '%s' not found", call.name());
      if (is(Flag.FAIL_FAST)) throw new AssertionError(message);
      return;
    }

    if (is(Flag.DRY_RUN)) return;

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
      var result = logbook().add(call, normal, errors, duration, code);
      log(Level.DEBUG, "%s finished after %d ms", tool.name(), duration.toMillis());

      if (code == 0) return;

      var caption = log(Level.ERROR, "%s failed with exit code %d", tool.name(), code);
      var message = new StringJoiner(System.lineSeparator());
      message.add(caption);
      result.toStrings().forEach(message::add);
      if (is(Flag.FAIL_FAST)) throw new AssertionError(message);
    } catch (RuntimeException exception) {
      log(Level.ERROR, "%s failed throwing %s", tool.name(), exception);
      if (is(Flag.FAIL_FAST)) throw exception;
    } finally {
      currentThread.setContextClassLoader(currentContextLoader);
    }
  }

  public HttpClient computeHttpClient() {
    return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
  }

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
    if (is(Flag.INCLUDE_RESOURCES_IN_SOURCES_JAR)) {
      jar = jar.with(unit.resources(), (call, resource) -> call.with("-C", resource, "."));
    }
    for (var source : sources) {
      jar = jar.with("--release", source.release());
      jar = jar.with("-C", source.path(), ".");
    }
    return jar;
  }

  public Jar computeJarForMainModule(SourceUnit unit) {
    var jar = computeJarCall(main(), unit);
    if (is(Flag.INCLUDE_SOURCES_IN_MODULAR_JAR)) {
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

  public Javac computeJavacForTestPreview() {
    var release = Runtime.version().feature();
    var sources = project().sources();
    var units = sources.testPreview().units();
    var modulePath =
        Paths.joinExisting(base().modules(""), base().modules("test"), base().libraries());
    return Call.javac()
        .withModule(units.toNames(","))
        .with("--enable-preview")
        .with("--release", release)
        .with("-Xlint:-preview")
        .with("--module-version", project().version().toString() + "-test-preview")
        .with(units.toModuleSourcePaths(false), Javac::withModuleSourcePath)
        .with(
            units.toModulePatches(main().units()).entrySet(),
            (javac, patch) -> javac.withPatchModule(patch.getKey(), patch.getValue()))
        .with(modulePath, Javac::withModulePath)
        .withEncoding("UTF-8")
        .with("-parameters")
        .withRecommendedWarnings()
        .with("-d", base().classes("test-preview", release));
  }

  public Jar computeJarForTestModule(SourceUnit unit) {
    return computeJarCall(project().sources().testSources(), unit);
  }

  public Jar computeJarForTestPreviewModule(SourceUnit unit) {
    return computeJarCall(project().sources().testPreview(), unit);
  }

  public Jar computeJarCall(Realm<?> realm, SourceUnit unit) {
    var module = unit.name();
    var archive = project().toModuleArchive(realm.name(), module);
    var classes = base().classes(realm.name(), realm.release().feature(), module);
    var resources = new ArrayList<>(unit.resources()); // TODO Include main resources if patched
    return Call.jar()
        .with("--create")
        .withArchiveFile(archive)
        .with(unit.descriptor().mainClass(), Jar::withMainClass)
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
