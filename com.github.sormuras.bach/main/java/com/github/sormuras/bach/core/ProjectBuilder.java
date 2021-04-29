package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.CodeSpaceMain;
import com.github.sormuras.bach.api.CodeSpaceTest;
import com.github.sormuras.bach.api.DeclaredModule;
import com.github.sormuras.bach.api.DeclaredModuleFinder;
import com.github.sormuras.bach.api.DeclaredModuleReference;
import com.github.sormuras.bach.api.ExternalLibraryName;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ExternalModuleLocations;
import com.github.sormuras.bach.api.ExternalModuleLocator;
import com.github.sormuras.bach.api.Externals;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.ModulePaths;
import com.github.sormuras.bach.api.Option;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.SourceFolder;
import com.github.sormuras.bach.api.SourceFolders;
import com.github.sormuras.bach.api.Spaces;
import com.github.sormuras.bach.api.Tweak;
import com.github.sormuras.bach.api.Tweaks;
import com.github.sormuras.bach.api.external.JUnit;
import com.github.sormuras.bach.internal.ComposedPathMatcher;
import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.internal.Strings;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ProjectBuilder {

  protected final Logbook logbook;
  protected final Options options;

  public ProjectBuilder(Logbook logbook, Options options) {
    this.logbook = logbook;
    this.options = options;
  }

  public Project build() {
    logbook.log(Level.DEBUG, "Project object being built by " + getClass());
    logbook.log(Level.DEBUG, "Read values from options titled: " + options.title());
    var name = buildProjectName();
    var version = buildProjectVersion();
    var folders = buildFolders();
    var spaces = buildSpaces(folders);
    var externals = buildExternals();
    return new Project(name, version, folders, spaces, externals);
  }

  public String buildProjectName() {
    return options.get(Option.PROJECT_NAME);
  }

  public Version buildProjectVersion() {
    return Version.parse(options.get(Option.PROJECT_VERSION));
  }

  public Folders buildFolders() {
    return Folders.of(options.get(Option.CHROOT));
  }

  public Spaces buildSpaces(Folders folders) {
    logbook.log(Level.DEBUG, "Build code spaces object for " + folders);
    var root = folders.root();
    var paths = new ArrayList<>(Paths.findModuleInfoJavaFiles(root, 9));
    logbook.log(Level.DEBUG, "Build spaces for %d module(s)".formatted(paths.size()));

    var mainModules = new TreeMap<String, DeclaredModule>();
    var testModules = new TreeMap<String, DeclaredModule>();

    var mainMatcher = ComposedPathMatcher.ofGlobModules(options.list(Option.MAIN_MODULES_PATTERN));
    var testMatcher = ComposedPathMatcher.ofGlobModules(options.list(Option.TEST_MODULES_PATTERN));

    var jarWithSources = options.is(Option.MAIN_JAR_WITH_SOURCES);
    for (var path : paths) {
      if (Paths.countName(path, ".bach") >= 1) {
        logbook.log(Level.DEBUG, "Skip module %s - its path contains `.bach`".formatted(path));
        continue;
      }
      if (testMatcher.anyMatch(path)) {
        var local = buildDeclaredModule(root, path, jarWithSources);
        testModules.put(local.name(), local);
        continue;
      }
      if (mainMatcher.anyMatch(path)) {
        var local = buildDeclaredModule(root, path, jarWithSources);
        mainModules.put(local.name(), local);
        continue;
      }
      logbook.log(Level.DEBUG, "Skip module %s - no match for main nor test space".formatted(path));
    }

    var mainModulePaths = options.list(Option.MAIN_MODULE_PATH);
    var testModulePaths = options.list(Option.TEST_MODULE_PATH);

    var main =
        new CodeSpaceMain(
            new DeclaredModuleFinder(mainModules),
            buildDeclaredModulePaths(root, mainModulePaths),
            Integer.parseInt(options.get(Option.MAIN_JAVA_RELEASE)),
            buildTweaks(CodeSpace.MAIN));
    var test =
        new CodeSpaceTest(
            new DeclaredModuleFinder(testModules),
            buildDeclaredModulePaths(root, testModulePaths),
            buildTweaks(CodeSpace.TEST));

    return new Spaces(main, test);
  }

  public DeclaredModule buildDeclaredModule(Path root, Path path, boolean jarWithSources) {
    var info = Files.isDirectory(path) ? path.resolve("module-info.java") : path;
    var reference = DeclaredModuleReference.of(info);

    // no source folder, no resource folder, module declaration in root directory
    // "module-info.java"
    if (root.relativize(info).getNameCount() == 1) {
      var sources = new SourceFolders(List.of());
      var resources = new SourceFolders(List.of());
      return new DeclaredModule(root, reference, sources, resources);
    }

    // assume single source folder, directory and module names are equal
    // "foo.bar/module-info.java" with "module foo.bar {...}"
    var parent = info.getParent();
    if (Strings.name(parent).equals(reference.name())) {
      var folder = buildDeclaredSourceFolder(parent);
      var sources = new SourceFolders(List.of(folder));
      var resources = new SourceFolders(jarWithSources ? List.of(folder) : List.of());
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
    var release = SourceFolder.parseReleaseNumber(Strings.name(path));
    return new SourceFolder(path, release);
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

  public Tweaks buildTweaks(CodeSpace space) {
    var tweaks = new ArrayList<Tweak>();
    return new Tweaks(List.copyOf(tweaks));
  }

  public Externals buildExternals() {
    var requires = buildExternalsRequires();
    var locators = buildExternalsLocators();
    return new Externals(requires, locators);
  }

  public Set<String> buildExternalsRequires() {
    return Set.copyOf(options.list(Option.PROJECT_REQUIRES));
  }

  public List<ExternalModuleLocator> buildExternalsLocators() {
    var locators = new ArrayList<ExternalModuleLocator>();
    fillExternalsLocatorsFromOptionModuleLocation(locators);
    fillExternalsLocatorsFromOptionLibraryVersion(locators);
    return List.copyOf(locators);
  }

  public void fillExternalsLocatorsFromOptionModuleLocation(List<ExternalModuleLocator> locators) {
    var deque = new ArrayDeque<>(options.list(Option.EXTERNAL_MODULE_LOCATION));
    if (deque.isEmpty()) return;
    var locationMap = new TreeMap<String, ExternalModuleLocation>();
    while (!deque.isEmpty()) {
      var module = deque.removeFirst();
      var uri = deque.removeFirst();
      var old = locationMap.put(module, new ExternalModuleLocation(module, uri));
      if (old != null) logbook.log(Level.WARNING, "Replaced %s with -> %s".formatted(old, uri));
    }
    locators.add(new ExternalModuleLocations(Map.copyOf(locationMap)));
  }

  public void fillExternalsLocatorsFromOptionLibraryVersion(List<ExternalModuleLocator> locators) {
    var deque = new ArrayDeque<>(options.list(Option.EXTERNAL_LIBRARY_VERSION));
    if (deque.isEmpty()) return;
    while (!deque.isEmpty()) {
      var name = deque.removeFirst();
      var version = deque.removeFirst();
      //noinspection SwitchStatementWithTooFewBranches
      switch (ExternalLibraryName.ofCli(name)) {
        case JUNIT -> locators.add(JUnit.of(version));
      }
    }
  }
}
