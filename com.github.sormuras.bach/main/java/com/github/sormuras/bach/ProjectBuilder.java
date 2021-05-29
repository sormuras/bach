package com.github.sormuras.bach;

import com.github.sormuras.bach.api.CodeSpaceMain;
import com.github.sormuras.bach.api.CodeSpaceTest;
import com.github.sormuras.bach.api.DeclaredModule;
import com.github.sormuras.bach.api.DeclaredModuleFinder;
import com.github.sormuras.bach.api.DeclaredModuleReference;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ExternalModuleLocations;
import com.github.sormuras.bach.api.ExternalModuleLocator;
import com.github.sormuras.bach.api.Externals;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.ModulePaths;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.SourceFolder;
import com.github.sormuras.bach.api.SourceFolders;
import com.github.sormuras.bach.api.Spaces;
import com.github.sormuras.bach.api.Tools;
import com.github.sormuras.bach.api.Tweaks;
import com.github.sormuras.bach.internal.ComposedPathMatcher;
import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.locator.FXGL;
import com.github.sormuras.bach.locator.JUnit;
import com.github.sormuras.bach.locator.JavaFX;
import com.github.sormuras.bach.locator.SormurasModulesLocator;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProjectBuilder {

  protected final Configuration configuration;
  protected final Logbook logbook;
  protected final Options options;

  public ProjectBuilder(Configuration configuration) {
    this.configuration = configuration;
    this.logbook = configuration.logbook();
    this.options = configuration.options();
  }

  public Project build() {
    logbook.log(Level.DEBUG, "Project object being built by " + getClass());
    logbook.log(Level.DEBUG, "Read values from options with id: " + options.id());
    var name = buildProjectName();
    var version = buildProjectVersion();
    var folders = configuration.folders();
    var spaces = buildSpaces(folders);
    var tools = buildTools();
    var externals = buildExternals();
    return new Project(name, version, folders, spaces, tools, externals);
  }

  public String buildProjectName() {
    var name = options.projectName().orElse(null);
    if (name == null || name.equals(".")) {
      return Strings.nameOrElse(options.chrootOrDefault(), "noname");
    }
    return name;
  }

  public Version buildProjectVersion() {
    return options.projectVersion().orElseThrow();
  }

  public Spaces buildSpaces(Folders folders) {
    logbook.log(Level.DEBUG, "Build code spaces object for " + folders);
    var root = folders.root();
    var paths = new ArrayList<>(Paths.findModuleInfoJavaFiles(root, 9));
    var size = paths.size();
    var s = size == 1 ? "" : "s";
    logbook.log(Level.DEBUG, "Build spaces from %d module declaration%s".formatted(size, s));

    var mainModules = new TreeMap<String, DeclaredModule>();
    var testModules = new TreeMap<String, DeclaredModule>();

    var mainMatcher = ComposedPathMatcher.ofGlobModules(options.mainModulePatterns());
    var testMatcher = ComposedPathMatcher.ofGlobModules(options.testModulePatterns());

    var jarWithSources = options.mainJarWithSources();
    for (var path : paths) {
      if (Paths.countName(path, ".bach") >= 1) {
        logbook.log(Level.TRACE, "Skip module %s - its path contains `.bach`".formatted(path));
        continue;
      }
      if (testMatcher.anyMatch(path)) {
        var module = buildDeclaredModule(root, path, jarWithSources);
        logbook.log(Level.DEBUG, "Test module %s declared in %s".formatted(module.name(), path));
        testModules.put(module.name(), module);
        continue;
      }
      if (mainMatcher.anyMatch(path)) {
        var module = buildDeclaredModule(root, path, jarWithSources);
        mainModules.put(module.name(), module);
        logbook.log(Level.DEBUG, "Main module %s declared in %s".formatted(module.name(), path));
        continue;
      }
      logbook.log(Level.TRACE, "Skip module %s - no match for main nor test space".formatted(path));
    }

    var main =
        new CodeSpaceMain(
            new DeclaredModuleFinder(mainModules),
            buildDeclaredModulePaths(root, options.mainModulePaths()),
            options.mainJavaRelease().orElse(Runtime.version().feature()));
    var test =
        new CodeSpaceTest(
            new DeclaredModuleFinder(testModules),
            buildDeclaredModulePaths(root, options.testModulePaths()));

    logbook.log(Level.DEBUG, "Main space modules: %s".formatted(main.modules().toNames(", ")));
    logbook.log(Level.DEBUG, "Test space modules: %s".formatted(test.modules().toNames(", ")));

    return new Spaces(main, test);
  }

  public DeclaredModule buildDeclaredModule(Path root, Path path, boolean jarWithSources) {
    var info = Files.isDirectory(path) ? path.resolve("module-info.java") : path;
    var reference = DeclaredModuleReference.of(info);

    // no source folder, no resource folder, module declaration in root directory
    // "module-info.java"
    if (root.relativize(info).getNameCount() == 1) {
      var sources = SourceFolders.of();
      var resources = SourceFolders.of();
      return new DeclaredModule(root, reference, sources, resources);
    }

    // assume single source folder, directory and module names are equal
    // "foo.bar/module-info.java" with "module foo.bar {...}"
    var parent = info.getParent();
    if (Strings.name(parent).equals(reference.name())) {
      var folder = buildDeclaredSourceFolder(parent);
      var sources = SourceFolders.of(folder);
      var resources = jarWithSources ? SourceFolders.of(folder) : SourceFolders.of();
      return new DeclaredModule(parent, reference, sources, resources);
    }

    // sources = "java", "java-module", or targeted "java.*?(\d+)$"
    // resources = "resources", or targeted "resource.*?(\d+)$"
    var space = parent.getParent(); // usually "main", "test", or equivalent
    if (space == null) throw new AssertionError("No parents' parent? info -> " + info);
    var content = Paths.findNameOrElse(info, reference.name(), space.getParent());
    var sources = buildDeclaredSourceFolders(space, "java");
    var resources = buildDeclaredSourceFolders(space, jarWithSources ? "" : "resource");
    return new DeclaredModule(content, reference, sources, resources);
  }

  public ModulePaths buildDeclaredModulePaths(Path root, List<String> paths) {
    return new ModulePaths(paths.stream().map(root::resolve).toList());
  }

  public SourceFolder buildDeclaredSourceFolder(Path path) {
    if (Files.isRegularFile(path)) throw new IllegalArgumentException("Not a directory: " + path);
    return SourceFolder.of(path);
  }

  public SourceFolders buildDeclaredSourceFolders(Path path, String namePrefix) {
    var list =
        Paths.list(path, Files::isDirectory).stream()
            .filter(candidate -> Strings.name(candidate).startsWith(namePrefix))
            .map(this::buildDeclaredSourceFolder)
            .sorted(Comparator.comparingInt(SourceFolder::release))
            .toList();
    return new SourceFolders(list);
  }

  public Tools buildTools() {
    var limits = buildToolsLimits();
    var skips = buildToolsSkips();
    var tweaks = buildToolsTweaks();
    return new Tools(limits, skips, tweaks);
  }

  public Set<String> buildToolsLimits() {
    if (options.limitTools().isEmpty()) return Set.of();
    return Set.of(options.limitTools().get().split(","));
  }

  public Set<String> buildToolsSkips() {
    if (options.skipTools().isEmpty()) return Set.of();
    return Set.of(options.skipTools().get().split(","));
  }

  public Tweaks buildToolsTweaks() {
    return new Tweaks(options.tweaks().stream().toList());
  }

  public Externals buildExternals() {
    var requires = buildExternalsRequires();
    var locators = buildExternalsLocators();
    return new Externals(requires, locators);
  }

  public Set<String> buildExternalsRequires() {
    return Set.copyOf(options.projectRequires());
  }

  public List<ExternalModuleLocator> buildExternalsLocators() {
    var locators = new ArrayList<ExternalModuleLocator>();
    fillExternalsLocatorsFromServices(locators);
    fillExternalsLocatorsFromOptionModuleLocation(locators);
    fillExternalsLocatorsFromOptionLibraryVersion(locators);
    return List.copyOf(locators);
  }

  public void fillExternalsLocatorsFromServices(List<ExternalModuleLocator> locators) {
    ServiceLoader.load(configuration.layer(), ExternalModuleLocator.class).forEach(locators::add);
  }

  public void fillExternalsLocatorsFromOptionModuleLocation(List<ExternalModuleLocator> locators) {
    var externalModuleLocations = options.externalModuleLocations();
    if (externalModuleLocations.isEmpty()) return;
    var map =
        externalModuleLocations.stream()
            .collect(Collectors.toMap(ExternalModuleLocation::module, Function.identity()));
    locators.add(new ExternalModuleLocations(Map.copyOf(map)));
  }

  public void fillExternalsLocatorsFromOptionLibraryVersion(List<ExternalModuleLocator> locators) {
    var externalLibraryVersions = options.externalLibraryVersions();
    if (externalLibraryVersions.isEmpty()) return;
    for (var externalLibraryVersion : externalLibraryVersions) {
      var version = externalLibraryVersion.version();
      switch (externalLibraryVersion.name()) {
        case FXGL -> locators.add(FXGL.of(version));
        case JAVAFX -> locators.add(JavaFX.of(version));
        case JUNIT -> locators.add(JUnit.of(version));
        case SORMURAS_MODULES -> locators.add(new SormurasModulesLocator(version, configuration));
      }
    }
  }
}
