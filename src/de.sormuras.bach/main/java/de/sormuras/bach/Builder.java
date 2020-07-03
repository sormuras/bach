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

import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.internal.Resources;
import de.sormuras.bach.project.Base;
import de.sormuras.bach.project.Link;
import de.sormuras.bach.project.MainSources;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.SourceUnit;
import de.sormuras.bach.project.SourceUnits;
import de.sormuras.bach.tool.JUnit;
import de.sormuras.bach.tool.Jar;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
import de.sormuras.bach.tool.TestModule;
import java.io.File;
import java.lang.System.Logger.Level;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** An extensible build workflow. */
public class Builder {

  @FunctionalInterface
  public interface Factory {
    Builder create(Bach bach);
  }

  private final Bach bach;
  private HttpClient http;
  private final SormurasModulesProperties sormurasModulesProperties;

  public Builder(Bach bach) {
    this.bach = bach;
    this.sormurasModulesProperties = computeSormurasModulesProperties();
  }

  public final Bach bach() {
    return bach;
  }

  public final Project project() {
    return bach.project();
  }

  public final Base base() {
    return bach.project().base();
  }

  public final MainSources main() {
    return bach.project().sources().main();
  }

  public final HttpClient http() {
    if (http == null) {
      http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    }
    return http;
  }

  public void build() {
    buildLibrariesDirectoryByResolvingMissingExternalModules();
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

    if (project().sources().test().units().isPresent()) {
      buildTestModules();
      bach.printStatistics(Level.DEBUG, base().modules("test"));
      buildTestReportsByExecutingTestModules();
    }

    if (main().units().isPresent()) {
      bach.printStatistics(Level.INFO, base().modules(""));
    }
  }

  public void buildLibrariesDirectoryByResolvingMissingExternalModules() {
    // get external requires from all module-info.java files
    // get external modules from project descriptor
    // download them
    // get missing external modules from libraries directory
    // download them recursively

    var libraries = base().libraries();
    var declared = project().toDeclaredModuleNames();
    var resolver =
        new Resolver(List.of(libraries), declared, this::buildLibrariesDirectoryByResolvingModules);
    resolver.resolve(project().toRequiredModuleNames()); // from all module-info.java files
    resolver.resolve(project().library().requires()); // from project descriptor
  }

  public void buildLibrariesDirectoryByResolvingModules(Set<String> modules) {
    bach.logbook().log(Level.DEBUG, "Resolve missing external modules: " + modules);
    var resources = new Resources(http());
    for (var module : modules) {
      var optionalLink =
          project().library().findLink(module).or(() -> computeLinkForExternalModule(module));
      if (optionalLink.isEmpty()) {
        bach.logbook().log(Level.WARNING, "Module %s not locatable", module);
        continue;
      }
      var link = optionalLink.orElseThrow();
      var uri = link.toURI();
      var name = module + ".jar";
      try {
        var lib = Paths.createDirectories(base().libraries());
        var file = resources.copy(uri, lib.resolve(name));
        var size = Paths.size(file);
        bach.logbook().log(Level.INFO, "%,12d %-42s << %s", size, file, uri);
      } catch (Exception e) {
        throw new Error("Resolve module '" + module + "' failed: " + uri + "\n\t" + e, e);
      }
    }
  }

  public void buildMainModules() {
    var units = main().units();
    bach.logbook().log(Level.DEBUG, "Build of %d main module(s) started", units.size());
    bach.executeCall(computeJavacForMainSources());
    var modules = base().modules("");
    Paths.deleteDirectories(modules);
    Paths.createDirectories(modules);
    Paths.createDirectories(base().sources(""));

    for (var unit : units.units().values()) {
      bach.executeCall(computeJarForMainSources(unit));
      if (!unit.sources().isMultiTarget()) {
        bach.executeCall(computeJarForMainModule(unit));
        continue;
      }
      var module = unit.name();
      var mainClass = unit.descriptor().mainClass();
      for (var directory : unit.sources().directories()) {
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
        bach.executeCall(javac);
      }
      var sources = new ArrayDeque<>(unit.sources().directories());
      var sources0 = sources.removeFirst();
      var classes0 = base().classes("", sources0.release(), module);
      var jar =
          Call.jar()
              .with("--create")
              .withArchiveFile(toModuleArchive("", module))
              .with(mainClass.isPresent(), "--main-class", mainClass.orElse("?"))
              .with("-C", classes0, ".")
              .with(isJarModuleWithSources(), "-C", sources0.path(), ".");
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
                .with(isJarModuleWithSources(), "-C", source.path(), ".");
      }
      bach.executeCall(jar);
    }
  }

  public void buildApiDocumentation() {
    bach.executeCall(computeJavadocForMainSources());
    bach.executeCall(computeJarForApiDocumentation());
  }

  public void buildCustomRuntimeImage() {
    var modulePaths = toModulePaths(base().modules(""), base().libraries());
    var autos = Modules.findAutomaticModules(modulePaths);
    if (autos.size() > 0) {
      bach.logbook().log(Level.WARNING, "Automatic module(s) detected: %s", autos);
      return;
    }
    Paths.deleteDirectories(base().workspace("image"));
    var jlink = computeJLinkForCustomRuntimeImage();
    bach.executeCall(jlink);
  }

  public void buildTestModules() {
    var units = project().sources().test().units();
    bach.logbook().log(Level.DEBUG, "Build of %d test module(s) started", units.size());
    bach.executeCall(computeJavacForTestSources());
    Paths.createDirectories(base().modules("test"));
    units.toUnits().map(this::computeJarForTestModule).forEach(bach::executeCall);
  }

  public void buildTestReportsByExecutingTestModules() {
    var test = project().sources().test();
    for (var unit : test.units().units().values())
      buildTestReportsByExecutingTestModule("test", unit);
  }

  public void buildTestReportsByExecutingTestModule(String realm, SourceUnit unit) {
    var module = unit.name();
    var modulePaths =
        toModulePaths(
            toModuleArchive(realm, module), // test module
            base().modules(""), // main modules
            base().modules(realm), // other test modules
            base().libraries()); // external modules
    bach.logbook().log(Level.DEBUG, "Run tests in '%s' with module-path: %s", module, modulePaths);

    var testModule = new TestModule(module, modulePaths);
    if (testModule.findProvider().isPresent()) bach.executeCall(testModule);

    var junit = computeJUnitCall(realm, unit, modulePaths);
    if (junit.findProvider().isPresent()) bach.executeCall(junit);
  }

  public SormurasModulesProperties computeSormurasModulesProperties() {
    return new SormurasModulesProperties(Map.of());
  }

  public Optional<Link> computeLinkForExternalModule(String module) {
    return sormurasModulesProperties.lookup(module);
  }

  public Javac computeJavacForMainSources() {
    var release = main().release().feature();
    var modulePath = toModulePath(base().libraries());
    return Call.javac()
        .withModule(main().units().toNames(","))
        .with("--module-version", project().version())
        .with(toModuleSourcePaths(main().units(), false), Javac::withModuleSourcePath)
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
    var sources = new ArrayDeque<>(unit.sources().directories());
    var file = module + '@' + project().version() + "-sources.jar";
    var jar =
        Call.jar()
            .with("--create")
            .withArchiveFile(base().sources("").resolve(file))
            .with("--no-manifest")
            .with("-C", sources.removeFirst().path(), ".");
    if (isJarSourcesWithResources()) {
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
            .withArchiveFile(toModuleArchive("", module))
            .with(mainClass.isPresent(), "--main-class", mainClass.orElse("?"))
            .with("-C", classes, ".")
            .with(resources, (call, resource) -> call.with("-C", resource, "."));
    if (isJarModuleWithSources()) {
      jar = jar.with(unit.sources().directories(), (call, src) -> call.with("-C", src.path(), "."));
    }
    return jar;
  }

  public Javadoc computeJavadocForMainSources() {
    var modulePath = toModulePath(base().libraries());
    return Call.javadoc()
        .withModule(main().units().toNames(","))
        .with(toModuleSourcePaths(main().units(), false), Javadoc::withModuleSourcePath)
        .with(modulePath, Javadoc::withModulePath)
        .with("-d", base().documentation("api"))
        .withEncoding("UTF-8")
        .with("-locale", "en")
        .with("-quiet")
        .with("-Xdoclint")
        .with("--show-module-contents", "all");
  }

  public Jar computeJarForApiDocumentation() {
    var file = project().name() + '-' + project().version() + "-api.jar";
    return Call.jar()
        .with("--create")
        .withArchiveFile(base().documentation(file))
        .with("--no-manifest")
        .with("-C", base().documentation("api"), ".");
  }

  public Call<?> computeJLinkForCustomRuntimeImage() {
    var mainModule = Modules.findMainModule(main().units().toUnits().map(SourceUnit::descriptor));
    return Call.tool("jlink")
        .with("--add-modules", main().units().toNames(","))
        .with("--module-path", toModulePath(base().modules(""), base().libraries()).get(0))
        .with(mainModule.isPresent(), "--launcher", project().name() + '=' + mainModule.orElse("?"))
        .with("--compress", "2")
        .with("--no-header-files")
        .with("--no-man-pages")
        .with("--output", base().workspace("image"));
  }

  public Javac computeJavacForTestSources() {
    var release = Runtime.version().feature();
    var sources = project().sources();
    var units = sources.test().units();
    var modulePath = toModulePath(base().modules(""), base().libraries());
    return Call.javac()
        .withModule(units.toNames(","))
        .with("--module-version", project().version().toString() + "-test")
        .with(toModuleSourcePaths(units, false), Javac::withModuleSourcePath)
        .with(
            toModulePatches(units, main().units()).entrySet(),
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
        .withArchiveFile(toModuleArchive("test", module))
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

  public boolean isJarModuleWithSources() {
    return false;
  }

  public boolean isJarSourcesWithResources() {
    return false;
  }

  public Path toModuleArchive(String realm, String module) {
    return toModuleArchive(realm, module, project().version());
  }

  public Path toModuleArchive(String realm, String module, Version version) {
    var suffix = realm.isEmpty() ? "" : '-' + realm;
    return base().modules(realm).resolve(module + '@' + version + suffix + ".jar");
  }

  public List<String> toModulePath(Path... elements) {
    var paths = toModulePaths(elements);
    return paths.isEmpty() ? List.of() : List.of(Paths.join(paths));
  }

  public List<Path> toModulePaths(Path... elements) {
    var paths = new ArrayList<Path>();
    for (var element : elements) if (Files.exists(element)) paths.add(element);
    return List.copyOf(paths);
  }

  public List<String> toModuleSourcePaths(SourceUnits units, boolean forceModuleSpecificForm) {
    var paths = new ArrayList<String>();
    var patterns = new TreeSet<String>(); // "src:etc/*/java"
    var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
    for (var unit : units.units().values()) {
      var sourcePaths = unit.sources().toModuleSpecificSourcePaths();
      if (forceModuleSpecificForm) {
        specific.put(unit.name(), sourcePaths);
        continue;
      }
      try {
        for (var path : sourcePaths) patterns.add(toModuleSourcePathPatternForm(path, unit.name()));
      } catch (FindException e) {
        specific.put(unit.name(), sourcePaths);
      }
    }
    if (patterns.isEmpty() && specific.isEmpty()) throw new IllegalStateException("No forms?!");
    if (!patterns.isEmpty()) paths.add(String.join(File.pathSeparator, patterns));
    var entries = specific.entrySet();
    for (var entry : entries) paths.add(entry.getKey() + "=" + Paths.join(entry.getValue()));
    return List.copyOf(paths);
  }

  public String toModuleSourcePathPatternForm(Path info, String module) {
    var deque = new ArrayDeque<String>();
    for (var element : info.normalize()) {
      var name = element.toString();
      if (name.equals("module-info.java")) continue;
      deque.addLast(name.equals(module) ? "*" : name);
    }
    var pattern = String.join(File.separator, deque);
    if (!pattern.contains("*")) throw new FindException("Name '" + module + "' not found: " + info);
    if (pattern.equals("*")) return ".";
    if (pattern.endsWith("*")) return pattern.substring(0, pattern.length() - 2);
    if (pattern.startsWith("*")) return "." + File.separator + pattern;
    return pattern;
  }

  public Map<String, String> toModulePatches(SourceUnits units, SourceUnits upstream) {
    if (units.units().isEmpty() || upstream.isEmpty()) return Map.of();
    var patches = new TreeMap<String, String>();
    for (var unit : units.units().values()) {
      var module = unit.name();
      upstream
          .toUnits()
          .filter(up -> up.name().equals(module))
          .findAny()
          .ifPresent(up -> patches.put(module, up.sources().toModuleSpecificSourcePath()));
    }
    return patches;
  }

  /** Resolve missing external modules. */
  public static class Resolver {

    private final Path[] paths;
    private final Set<String> declared;
    private final Consumer<Set<String>> transporter;
    private final Set<String> system;

    public Resolver(List<Path> paths, Set<String> declared, Consumer<Set<String>> transporter) {
      this.paths = Objects.requireNonNull(paths, "paths").toArray(Path[]::new);
      this.declared = new TreeSet<>(Objects.requireNonNull(declared, "declared"));
      this.transporter = Objects.requireNonNull(transporter, "transporter");
      this.system = Modules.declared(ModuleFinder.ofSystem());
      if (paths.isEmpty()) throw new IllegalArgumentException("At least one path expected");
    }

    public void resolve(Set<String> required) {
      resolveModules(required);
      resolveLibraryModules();
    }

    public void resolveModules(Set<String> required) {
      var missing = missing(required);
      if (missing.isEmpty()) return;
      transporter.accept(missing);
      var unresolved = missing(required);
      if (unresolved.isEmpty()) return;
      throw new IllegalStateException("Unresolved modules: " + unresolved);
    }

    public void resolveLibraryModules() {
      do {
        var missing = missing(Modules.required(ModuleFinder.of(paths)));
        if (missing.isEmpty()) return;
        resolveModules(missing);
      } while (true);
    }

    Set<String> missing(Set<String> required) {
      var missing = new TreeSet<>(required);
      missing.removeAll(declared);
      if (required.isEmpty()) return Set.of();
      missing.removeAll(system);
      if (required.isEmpty()) return Set.of();
      var library = Modules.declared(ModuleFinder.of(paths));
      missing.removeAll(library);
      return missing;
    }
  }

  /** https://github.com/sormuras/modules */
  public class SormurasModulesProperties {

    private Map<String, String> moduleMaven;
    private Map<String, String> moduleVersion;
    private final Map<String, String> variants;

    public SormurasModulesProperties(Map<String, String> variants) {
      this.variants = variants;
    }

    public Optional<Link> lookup(String module) {
      if (moduleMaven == null && moduleVersion == null)
        try {
          var resources = new Resources(http());
          moduleMaven = load(resources, "module-maven.properties");
          moduleVersion = load(resources, "module-version.properties");
        } catch (Exception e) {
          throw new RuntimeException("Load module properties failed", e);
        }
      if (moduleMaven == null) throw new IllegalStateException("module-maven map is null");
      if (moduleVersion == null) throw new IllegalStateException("module-version map is null");

      var maven = moduleMaven.get(module);
      if (maven == null) return Optional.empty();
      var indexOfColon = maven.indexOf(':');
      if (indexOfColon < 0) throw new AssertionError("Expected group:artifact, but got: " + maven);
      var version = variants.getOrDefault(module, moduleVersion.get(module));
      if (version == null) return Optional.empty();
      var group = maven.substring(0, indexOfColon);
      var artifact = maven.substring(indexOfColon + 1);
      return Optional.of(Link.ofCentral(module, group, artifact, version));
    }

    private static final String ROOT = "https://github.com/sormuras/modules";

    private Map<String, String> load(Resources resources, String properties) throws Exception {
      var root = Path.of(System.getProperty("user.home", ""));
      var cache = Files.createDirectories(root.resolve(".bach/modules"));
      var source = URI.create(String.join("/", ROOT, "raw/master", properties));
      var target = cache.resolve(properties);
      var path = resources.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
      return map(load(new Properties(), path));
    }

    /** Load all strings from the specified file into the passed properties instance. */
    private Properties load(Properties properties, Path path) {
      if (Files.isRegularFile(path)) {
        try (var reader = Files.newBufferedReader(path)) {
          properties.load(reader);
        } catch (Exception e) {
          throw new RuntimeException("Load properties failed: " + path, e);
        }
      }
      return properties;
    }

    /** Convert all {@link String}-based properties into a {@code Map<String, String>}. */
    private Map<String, String> map(Properties properties) {
      var map = new TreeMap<String, String>();
      for (var name : properties.stringPropertyNames()) {
        map.put(name, properties.getProperty(name));
      }
      return map;
    }
  }
}
