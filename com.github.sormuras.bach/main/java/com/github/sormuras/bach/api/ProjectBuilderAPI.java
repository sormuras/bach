package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Options.Property;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.lookup.ModuleLookup;
import com.github.sormuras.bach.project.Libraries;
import com.github.sormuras.bach.project.Settings;
import com.github.sormuras.bach.project.Spaces;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Methods related to building projects. */
public interface ProjectBuilderAPI {

  Bach bach();

  default Project computeProject() {
    var info = computeProjectInfo();
    var settings = computeProjectSettings(info);
    var libraries = computeProjectLibraries(info, settings);
    var spaces = computeProjectSpaces(info, settings, libraries);
    return new Project(settings, libraries, spaces);
  }

  default ProjectInfo computeProjectInfo() {
    var info = getClass().getModule().getAnnotation(ProjectInfo.class);
    if (info != null) return info;
    return Bach.class.getModule().getAnnotation(ProjectInfo.class);
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
    return new Libraries(requires, List.copyOf(lookups));
  }

  default ModuleLookup computeProjectModuleLookup(ProjectInfo.External external) {
    var module = external.module();
    var target = external.via();
    return switch (external.type()) {
      case AUTO -> ModuleLookup.external(module).via(target);
      case URI -> ModuleLookup.external(module).viaUri(target);
      case PATH -> ModuleLookup.external(module).viaPath(external.pathBase(), target);
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

  default Spaces computeProjectSpaces(ProjectInfo info, Settings settings, Libraries libraries) {
    return new Spaces(info.format());
  }

  default String computeMainJarFileName(String module) {
    return module + '@' + bach().project().versionNumberAndPreRelease() + ".jar";
  }

  default void buildProject() throws Exception {
    var bach = bach();
    var project = bach.project();
    bach.print("Build %s %s", project.name(), project.version());
    if (bach.is(Options.Flag.VERBOSE)) bach.info();
    var start = Instant.now();
    if (bach.is(Options.Flag.STRICT)) bach.formatJavaSourceFiles(JavaFormatterAPI.Mode.VERIFY);
    bach.loadMissingExternalModules();
    buildProjectMainSpace();
    buildProjectTestSpace();
    bach.print("Build took %s", Strings.toString(Duration.between(start, Instant.now())));
  }

  default void buildProjectMainSpace() throws Exception {}

  default void buildProjectTestSpace() throws Exception {}
}
