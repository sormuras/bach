package test.projects.builder;

import com.github.sormuras.bach.Core;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProjectBuilder {

  protected final Core core;
  protected final Logbook logbook;
  protected final Options options;

  public ProjectBuilder(Core core) {
    this.core = core;
    this.logbook = core.logbook();
    this.options = core.options();
  }

  public Project build() {
    logbook.log(Level.DEBUG, "Project object being built by " + getClass());
    logbook.log(Level.DEBUG, "Read values from options with id: " + options);
    var name = buildProjectName();
    var version = buildProjectVersion();
    var folders = core.folders();
    var spaces = buildSpaces(folders);
    var tools = buildTools();
    var externals = buildExternals();
    return new Project(name, version, folders, spaces, tools, externals);
  }

  public String buildProjectName() {
    var name = options.project_name();
    if (name == null || name.equals(".")) {
      return Paths.nameOrElse(options.chroot(), "noname");
    }
    return name;
  }

  public Version buildProjectVersion() {
    return options.project_version();
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

    var mainMatcher = ComposedPathMatcher.ofGlobModules(options.main_module_pattern());
    var testMatcher = ComposedPathMatcher.ofGlobModules(options.test_module_pattern());

    for (var path : paths) {
      var name = ".bach";
      if (Paths.countName(path, name) >= 1) {
        logbook.log(Level.TRACE, "Skip module %s - its path contains %s".formatted(path, name));
        continue;
      }
      if (testMatcher.anyMatch(path)) {
        var module = buildDeclaredModule(root, path, true);
        logbook.log(Level.DEBUG, "Test module %s declared in %s".formatted(module.name(), path));
        testModules.put(module.name(), module);
        continue;
      }
      if (mainMatcher.anyMatch(path)) {
        var module = buildDeclaredModule(root, path, options.main_jar_with_sources());
        mainModules.put(module.name(), module);
        logbook.log(Level.DEBUG, "Main module %s declared in %s".formatted(module.name(), path));
        continue;
      }
      logbook.log(Level.TRACE, "Skip module %s - no match for main nor test space".formatted(path));
    }

    var main =
        new CodeSpaceMain(
            new DeclaredModuleFinder(mainModules),
            buildDeclaredModulePaths(root, options.main_module_path()),
            options.main_java_release());
    var test =
        new CodeSpaceTest(
            new DeclaredModuleFinder(testModules),
            buildDeclaredModulePaths(root, options.test_module_path()));

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
    if (Paths.name(parent).equals(reference.name())) {
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
            .filter(candidate -> Paths.name(candidate).startsWith(namePrefix))
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
    if (options.limit_tool().isEmpty()) return Set.of();
    return Set.copyOf(options.limit_tool());
  }

  public Set<String> buildToolsSkips() {
    if (options.skip_tool().isEmpty()) return Set.of();
    return Set.copyOf(options.skip_tool());
  }

  public Tweaks buildToolsTweaks() {
    return new Tweaks(options.tweak().stream().toList());
  }

  public Externals buildExternals() {
    var requires = buildExternalsRequires();
    var locators = buildExternalsLocators();
    return new Externals(requires, locators);
  }

  public Set<String> buildExternalsRequires() {
    return Set.copyOf(options.project_requires());
  }

  public List<ExternalModuleLocator> buildExternalsLocators() {
    var locators = new ArrayList<ExternalModuleLocator>();
    fillExternalsLocatorsFromOptionModuleLocation(locators);
    fillExternalsLocatorsFromOptionLibraryVersion(locators);
    return List.copyOf(locators);
  }

  public void fillExternalsLocatorsFromOptionModuleLocation(List<ExternalModuleLocator> locators) {
    var externalModuleLocations = options.external_module_location();
    if (externalModuleLocations.isEmpty()) return;
    var map =
        externalModuleLocations.stream()
            .collect(Collectors.toMap(ExternalModuleLocation::module, Function.identity()));
    locators.add(new ExternalModuleLocations(Map.copyOf(map)));
  }

  public void fillExternalsLocatorsFromOptionLibraryVersion(List<ExternalModuleLocator> locators) {
    var externalLibraryVersions = options.external_library_version();
    if (externalLibraryVersions.isEmpty()) return;
    for (var externalLibraryVersion : externalLibraryVersions) {
      var version = externalLibraryVersion.version();
      switch (externalLibraryVersion.name()) {
        case FXGL -> locators.add(FXGL.of(version));
        case JAVAFX -> locators.add(JavaFX.of(version));
        case JUNIT -> locators.add(JUnit.of(version));
        case SORMURAS_MODULES -> locators.add(new SormurasModulesLocator(core, version));
      }
    }
  }
}
