package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Options.Property;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.lookup.ModuleLookup;
import com.github.sormuras.bach.lookup.ModuleMetadata;
import com.github.sormuras.bach.project.Libraries;
import com.github.sormuras.bach.project.MainSpace;
import com.github.sormuras.bach.project.ModuleDeclaration;
import com.github.sormuras.bach.project.ModuleDeclarations;
import com.github.sormuras.bach.project.ModuleInfoReference;
import com.github.sormuras.bach.project.ModulePaths;
import com.github.sormuras.bach.project.Settings;
import com.github.sormuras.bach.project.SourceFolder;
import com.github.sormuras.bach.project.SourceFolders;
import com.github.sormuras.bach.project.Spaces;
import com.github.sormuras.bach.project.TestSpace;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Methods related to building projects. */
public interface ProjectComputerAPI {

  Bach bach();

  default Project computeProject() {
    var info = computeProjectInfo();
    var settings = computeProjectSettings(info);
    var libraries = computeProjectLibraries(info, settings);
    var spaces = computeProjectSpaces(info, settings, libraries);
    return new Project(settings, libraries, spaces);
  }

  default ProjectInfo computeProjectInfo() {
    return bach().options().info().orElse(Bach.class.getModule().getAnnotation(ProjectInfo.class));
  }

  default Settings computeProjectSettings(ProjectInfo info) {
    var root = bach().get(Property.PROJECT_ROOT, "");
    var name = bach().get(Property.PROJECT_NAME).orElseGet(() -> computeProjectName(info, root));
    var version = bach().get(Property.PROJECT_VERSION).orElseGet(() -> computeProjectVersion(info));
    return Settings.of(root, name, version);
  }

  default String computeProjectName(ProjectInfo info, String base) {
    var name = info.name();
    if (!name.equals("*")) return name;
    return Path.of(base).toAbsolutePath().getFileName().toString();
  }

  default String computeProjectVersion(ProjectInfo info) {
    return info.version();
  }

  default Libraries computeProjectLibraries(ProjectInfo info, Settings settings) {
    var requires = Set.of(info.requires());
    bach().debug("Computed additionally required modules: %d", requires.size());
    var lookups = new ArrayList<ModuleLookup>();
    for (var external : info.lookupExternal()) lookups.add(computeProjectModuleLookup(external));
    for (var externals : info.lookupExternals()) lookups.add(computeProjectModuleLookup(externals));
    bach().debug("Computed module lookup functions: %s", lookups.size());
    var metamap =
        Arrays.stream(info.metadata())
            .map(this::computeProjectModuleMetadata)
            .collect(Collectors.toMap(ModuleMetadata::name, Function.identity()));
    bach().debug("External modules directory present: %s", settings.folders().externalModules());
    return new Libraries(requires, List.copyOf(lookups), Map.copyOf(metamap));
  }

  default ModuleLookup computeProjectModuleLookup(ProjectInfo.External external) {
    var module = external.module();
    var target = external.via();
    return switch (external.type()) {
      case AUTO -> ModuleLookup.external(module).via(target);
      case URI -> ModuleLookup.external(module).viaUri(target);
      case MAVEN -> ModuleLookup.external(module).viaMaven(external.mavenRepository(), target);
    };
  }

  default ModuleLookup computeProjectModuleLookup(ProjectInfo.Externals externals) {
    var version = externals.version();
    return switch (externals.name()) {
      case GITHUB_RELEASES -> ModuleLookup.ofGitHubReleases(bach());
      case JAVAFX -> ModuleLookup.ofJavaFX(version);
      case JUNIT -> ModuleLookup.ofJUnit(version);
      case LWJGL -> ModuleLookup.ofLWJGL(version);
      case SORMURAS_MODULES -> ModuleLookup.ofSormurasModules(bach(), version);
    };
  }

  default ModuleMetadata computeProjectModuleMetadata(ProjectInfo.Metadata metadata) {
    var checksums =
        Arrays.stream(metadata.checksums())
            .map(md -> new ModuleMetadata.Checksum(md.algorithm(), md.value()))
            .toList();
    return new ModuleMetadata(metadata.module(), metadata.size(), checksums);
  }

  default Spaces computeProjectSpaces(ProjectInfo info, Settings settings, Libraries libraries) {
    var root = settings.folders().root();
    var paths = new ArrayList<>(Paths.findModuleInfoJavaFiles(root, 9));

    var testDeclarations = new TreeMap<String, ModuleDeclaration>();
    var mainDeclarations = new TreeMap<String, ModuleDeclaration>();

    var iterator = paths.listIterator();
    while (iterator.hasNext()) {
      var path = iterator.next();
      if (path.startsWith(".bach")) continue;
      var reference = ModuleInfoReference.of(path);
      if (computeProjectSpaceTestMembership(reference)) {
        var declaration = computeProjectModuleDeclaration(root, path, false);
        testDeclarations.put(declaration.name(), declaration);
        iterator.remove();
        continue;
      }
      if (computeProjectSpaceMainMembership(reference)) {
        var declaration = computeProjectModuleDeclaration(root, path, false);
        mainDeclarations.put(declaration.name(), declaration);
        iterator.remove();
      }
    }

    var release =
        bach().get(Property.PROJECT_TARGETS_JAVA).stream()
            .mapToInt(Integer::parseInt)
            .findFirst()
            .orElseGet(info::compileModulesForJavaRelease);
    var main =
        new MainSpace(
            new ModuleDeclarations(mainDeclarations),
            computeProjectModulePaths(root, info.modulePaths()),
            release,
            computeProjectTweaks(info.tweaks()));
    var test =
        new TestSpace(
            new ModuleDeclarations(testDeclarations),
            computeProjectModulePaths(root, info.testModulePaths()),
            computeProjectTweaks(info.testTweaks()));
    return new Spaces(info.format(), main, test);
  }

  default boolean computeProjectSpaceTestMembership(ModuleInfoReference reference) {
    return reference.name().startsWith("test.") || Paths.countName(reference.info(), "test") == 1;
  }

  default boolean computeProjectSpaceMainMembership(ModuleInfoReference reference) {
    return true;
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

  default ModuleDeclaration computeProjectModuleDeclaration(
      Path root, Path path, boolean treatSourcesAsResources) {
    var info = Files.isDirectory(path) ? path.resolve("module-info.java") : path;
    var reference = ModuleInfoReference.of(info);

    // no source folder, module declaration in root directory
    // "module-info.java"
    if (root.relativize(info).getNameCount() == 1)
      return new ModuleDeclaration(
          reference, new SourceFolders(List.of()), new SourceFolders(List.of()));

    // assume single source folder, directory and module names are equal
    // "foo.bar/module-info.java" with "module foo.bar {...}"
    var parent = info.getParent();
    if (Paths.name(parent).equals(reference.name())) {
      var folder = computeProjectSourceFolder(parent);
      return new ModuleDeclaration(
          reference,
          new SourceFolders(List.of(folder)),
          new SourceFolders(treatSourcesAsResources ? List.of(folder) : List.of()));
    }
    // sources = "java", "java-module", or targeted "java.*?(\d+)$"
    // resources = "resources", or targeted "resource.*?(\d+)$"
    var space = parent.getParent(); // usually "main", "test", or equivalent
    if (space == null) throw new AssertionError("No parents' parent? info -> " + info);
    var sources = computeProjectSourceFolders(space, "java");
    var resources = computeProjectSourceFolders(space, treatSourcesAsResources ? "" : "resource");
    return new ModuleDeclaration(reference, sources, resources);
  }

  default ModulePaths computeProjectModulePaths(Path root, String... paths) {
    return new ModulePaths(Arrays.stream(paths).map(root::resolve).toList());
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
