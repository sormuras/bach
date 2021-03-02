package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Options.Property;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.lookup.ModuleLookup;
import com.github.sormuras.bach.lookup.ModuleMetadata;
import com.github.sormuras.bach.project.Libraries;
import com.github.sormuras.bach.project.MainSpace;
import com.github.sormuras.bach.project.Settings;
import com.github.sormuras.bach.project.Spaces;
import com.github.sormuras.bach.project.TestSpace;
import com.github.sormuras.bach.project.Tweak;
import com.github.sormuras.bach.project.Tweaks;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    var lookups = new ArrayList<ModuleLookup>();
    for (var external : info.lookupExternal()) lookups.add(computeProjectModuleLookup(external));
    for (var externals : info.lookupExternals()) lookups.add(computeProjectModuleLookup(externals));
    var metamap =
        Arrays.stream(info.metadata())
            .map(this::computeProjectModuleMetadata)
            .collect(Collectors.toMap(ModuleMetadata::name, Function.identity()));
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
    var main =
        new MainSpace(
            computeProjectSpaceModules(info.modules()),
            Arrays.stream(info.modulePaths()).map(root::resolve).toList(),
            info.compileModulesForJavaRelease(),
            computeProjectTweaks(info.tweaks()));
    var test =
        new TestSpace(
            computeProjectSpaceModules(info.testModules()),
            Arrays.stream(info.testModulePaths()).map(root::resolve).toList(),
            computeProjectTweaks(info.testTweaks()));
    return new Spaces(info.format(), main, test);
  }

  default List<String> computeProjectSpaceModules(String... modules) {
    if (modules.length == 1 && "*".equals(modules[0])) return List.of();
    return List.of(modules);
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

  default String computeMainJarFileName(String module) {
    return module + '@' + bach().project().versionNumberAndPreRelease() + ".jar";
  }
}
