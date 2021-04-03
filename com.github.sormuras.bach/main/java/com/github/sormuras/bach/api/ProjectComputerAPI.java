package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.internal.ComposedPathMatcher;
import com.github.sormuras.bach.lookup.ModuleLookup;
import com.github.sormuras.bach.lookup.ModuleMetadata;
import com.github.sormuras.bach.project.Flag;
import com.github.sormuras.bach.project.Libraries;
import com.github.sormuras.bach.project.LocalModule;
import com.github.sormuras.bach.project.LocalModules;
import com.github.sormuras.bach.project.MainSpace;
import com.github.sormuras.bach.project.ModuleInfoReference;
import com.github.sormuras.bach.project.ModuleLauncher;
import com.github.sormuras.bach.project.ModulePaths;
import com.github.sormuras.bach.project.Property;
import com.github.sormuras.bach.project.Settings;
import com.github.sormuras.bach.project.SourceFolder;
import com.github.sormuras.bach.project.SourceFolders;
import com.github.sormuras.bach.project.Spaces;
import com.github.sormuras.bach.project.TestSpace;
import com.github.sormuras.bach.project.Tools;
import com.github.sormuras.bach.project.Tweak;
import com.github.sormuras.bach.project.Tweaks;
import com.github.sormuras.bach.util.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Methods related to building projects. */
public interface ProjectComputerAPI extends API {

  default Project computeProject() {
    var info = computeProjectInfo();
    var settings = computeProjectSettings(info);
    var libraries = computeProjectLibraries(info, settings);
    var spaces = computeProjectSpaces(info, settings);
    return new Project(settings, libraries, spaces);
  }

  default ProjectInfo computeProjectInfo() {
    return bach().options().info().orElse(Bach.class.getModule().getAnnotation(ProjectInfo.class));
  }

  default Settings computeProjectSettings(ProjectInfo info) {
    var root = "";
    var name = computeProjectName(info, root);
    var version = computeProjectVersion(info);
    var tools = new Tools(computeProjectToolsLimit(info), computeProjectToolsSkip(info));
    return Settings.of(root, name, version, tools);
  }

  default String computeProjectName(ProjectInfo info, String root) {
    var value = bach().options().find(Property.PROJECT_NAME);
    if (value.isPresent()) return value.get();
    var name = info.name();
    if (!name.equals("*")) return name;
    return Paths.nameOrElse(Path.of(root), "noname");
  }

  default String computeProjectVersion(ProjectInfo info) {
    return bach().options().find(Property.PROJECT_VERSION).orElseGet(info::version);
  }

  default int computeProjectTargetsJava(ProjectInfo info) {
    return bach()
        .options()
        .find(Property.PROJECT_TARGETS_JAVA)
        .map(Integer::parseInt)
        .orElseGet(info::compileModulesForJavaRelease);
  }

  default boolean computeProjectJarWithSources(ProjectInfo info) {
    return bach().is(Flag.JAR_WITH_SOURCES) || info.includeSourceFilesIntoModules();
  }

  default Set<String> computeProjectToolsLimit(ProjectInfo info) {
    return new TreeSet<>(
        bach()
            .options()
            .findRepeatable(Property.LIMIT_TOOLS)
            .orElseGet(() -> List.of(info.tools().limit())));
  }

  default Set<String> computeProjectToolsSkip(ProjectInfo info) {
    return new TreeSet<>(
        bach()
            .options()
            .findRepeatable(Property.SKIP_TOOLS)
            .orElseGet(() -> List.of(info.tools().skip())));
  }

  default Libraries computeProjectLibraries(ProjectInfo info, Settings settings) {
    var libraries = info.libraries();
    var requires = Set.of(libraries.requires());
    log("Computed additionally required modules: %d", requires.size());
    var lookups = new ArrayList<ModuleLookup>();
    for (var mod : libraries.externalModules()) lookups.add(computeProjectModuleLookup(mod));
    for (var lib : libraries.externalLibraries()) lookups.add(computeProjectModuleLookup(lib));
    if (bach().is(Flag.GUESS)) {
      say("Guess external modules by looking them up from GitHub Releases");
      lookups.add(ModuleLookup.ofGitHubReleases(bach()));
      say("Guess external modules by looking them up via sormuras/modules 0-ea");
      lookups.add(ModuleLookup.ofSormurasModules(bach(), "0-ea"));
    }
    log("Computed module lookup functions: %s", lookups.size());
    var metamap =
        Arrays.stream(libraries.metadata())
            .map(this::computeProjectModuleMetadata)
            .collect(Collectors.toMap(ModuleMetadata::name, Function.identity()));
    log("External modules directory present: %s", settings.folders().externalModules());
    return new Libraries(requires, List.copyOf(lookups), Map.copyOf(metamap));
  }

  default ModuleLookup computeProjectModuleLookup(ProjectInfo.Libraries.ExternalModule external) {
    var module = external.named();
    var target = external.via();
    return switch (external.type()) {
      case AUTO -> ModuleLookup.external(module).via(target);
      case URI -> ModuleLookup.external(module).viaUri(target);
      case MAVEN -> ModuleLookup.external(module).viaMaven(external.mavenRepository(), target);
    };
  }

  default ModuleLookup computeProjectModuleLookup(ProjectInfo.Libraries.ExternalLibrary library) {
    var version = library.version();
    return switch (library.named()) {
      case FXGL -> ModuleLookup.ofFXGL(bach(), version);
      case GITHUB_RELEASES -> ModuleLookup.ofGitHubReleases(bach());
      case JAVAFX -> ModuleLookup.ofJavaFX(version);
      case JUNIT -> ModuleLookup.ofJUnit(version);
      case LWJGL -> ModuleLookup.ofLWJGL(version);
      case SORMURAS_MODULES -> ModuleLookup.ofSormurasModules(bach(), version);
    };
  }

  default ModuleMetadata computeProjectModuleMetadata(ProjectInfo.Libraries.Metadata metadata) {
    var checksums =
        Arrays.stream(metadata.checksums())
            .map(md -> new ModuleMetadata.Checksum(md.algorithm(), md.value()))
            .toList();
    return new ModuleMetadata(metadata.module(), metadata.size(), checksums);
  }

  default Spaces computeProjectSpaces(ProjectInfo info, Settings settings) {
    var root = settings.folders().root();
    var paths = new ArrayList<>(Paths.findModuleInfoJavaFiles(root, 9));
    log("Compute spaces out of %d module%s", paths.size(), paths.size() == 1 ? "" : "s");

    var mainModules = new TreeMap<String, LocalModule>();
    var testModules = new TreeMap<String, LocalModule>();

    var mainMatcher = ComposedPathMatcher.of("glob", "module-info.java", info.modules());
    var testMatcher = ComposedPathMatcher.of("glob", "module-info.java", info.testModules());

    var treatSourcesAsResources = computeProjectJarWithSources(info);
    for (var path : paths) {
      if (Paths.countName(path, ".bach") >= 1) {
        log("Skip module %s - it contains `.bach` in its path names", path);
        continue;
      }
      if (testMatcher.anyMatch(path)) {
        var local = computeProjectLocalModule(root, path, treatSourcesAsResources);
        testModules.put(local.name(), local);
        continue;
      }
      if (mainMatcher.anyMatch(path)) {
        var local = computeProjectLocalModule(root, path, treatSourcesAsResources);
        mainModules.put(local.name(), local);
        continue;
      }
      log("Skip module %s - no match for main nor test space", path);
    }

    var main =
        new MainSpace(
            new LocalModules(mainModules),
            computeProjectModulePaths(root, info.modulePaths()),
            computeProjectTargetsJava(info),
            computeProjectModuleLauncher(info, settings, mainModules.values().stream()),
            computeProjectTweaks(info.tweaks()));
    var test =
        new TestSpace(
            new LocalModules(testModules),
            computeProjectModulePaths(root, info.testModulePaths()),
            computeProjectTweaks(info.testTweaks()));
    return new Spaces(info.formatSourceFilesWithCodeStyle(), main, test);
  }

  default SourceFolder computeProjectSourceFolder(Path path) {
    if (Files.isRegularFile(path)) throw new IllegalArgumentException("Not a directory: " + path);
    var release = SourceFolder.parseReleaseNumber(Paths.name(path));
    return new SourceFolder(path, release);
  }

  default SourceFolders computeProjectSourceFolders(Path path, String namePrefix) {
    var list =
        Paths.list(path, Files::isDirectory).stream()
            .filter(candidate -> Paths.name(candidate).startsWith(namePrefix))
            .map(this::computeProjectSourceFolder)
            .sorted(Comparator.comparingInt(SourceFolder::release))
            .toList();
    return new SourceFolders(list);
  }

  default LocalModule computeProjectLocalModule(
      Path root, Path path, boolean treatSourcesAsResources) {
    var info = Files.isDirectory(path) ? path.resolve("module-info.java") : path;
    var reference = ModuleInfoReference.of(info);

    // no source folder, no resource folder, module declaration in root directory
    // "module-info.java"
    if (root.relativize(info).getNameCount() == 1) {
      var sources = new SourceFolders(List.of());
      var resources = new SourceFolders(List.of());
      return new LocalModule(root, reference, sources, resources);
    }

    // assume single source folder, directory and module names are equal
    // "foo.bar/module-info.java" with "module foo.bar {...}"
    var parent = info.getParent();
    if (Paths.name(parent).equals(reference.name())) {
      var folder = computeProjectSourceFolder(parent);
      var sources = new SourceFolders(List.of(folder));
      var resources = new SourceFolders(treatSourcesAsResources ? List.of(folder) : List.of());
      return new LocalModule(parent, reference, sources, resources);
    }
    // sources = "java", "java-module", or targeted "java.*?(\d+)$"
    // resources = "resources", or targeted "resource.*?(\d+)$"
    var space = parent.getParent(); // usually "main", "test", or equivalent
    if (space == null) throw new AssertionError("No parents' parent? info -> " + info);
    var content = Paths.findNameOrElse(info, reference.name(), space.getParent());
    var sources = computeProjectSourceFolders(space, "java");
    var resources = computeProjectSourceFolders(space, treatSourcesAsResources ? "" : "resource");
    return new LocalModule(content, reference, sources, resources);
  }

  default ModulePaths computeProjectModulePaths(Path root, String... paths) {
    return new ModulePaths(Arrays.stream(paths).map(root::resolve).toList());
  }

  default Optional<ModuleLauncher> computeProjectModuleLauncher(
      ProjectInfo info, Settings settings, Stream<LocalModule> declarations) {
    var launcher = info.launcher();
    var command = launcher.command().equals("*") ? settings.name() : launcher.command();
    if (!launcher.module().equals("*")) {
      return Optional.of(new ModuleLauncher(command, launcher.module(), launcher.mainClass()));
    }
    var modules =
        declarations
            .map(d -> d.reference().descriptor())
            .filter(d -> d.mainClass().isPresent())
            .toList();
    if (modules.size() != 1) {
      log("No or multiple main modules declare a main class: %s", modules);
      return Optional.empty();
    }
    var moduleLauncher = new ModuleLauncher(command, modules.get(0).name(), "");
    log("Module launcher computed: %s", moduleLauncher);
    return Optional.of(moduleLauncher);
  }

  default Tweaks computeProjectTweaks(ProjectInfo.Tweak... infos) {
    var tweaks = new ArrayList<Tweak>();
    for (var tweak : infos) {
      var arguments = new ArrayList<String>();
      arguments.add(tweak.option());
      arguments.addAll(List.of(tweak.value()));
      tweaks.add(new Tweak(tweak.tool(), List.copyOf(arguments)));
    }
    return new Tweaks(List.copyOf(tweaks));
  }
}
