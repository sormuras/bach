package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Libraries;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Options.Property;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.lookup.ModuleLookup;
import java.lang.module.ModuleDescriptor.Version;
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
    var name = bach().get(Property.PROJECT_NAME).orElseGet(() -> computeProjectName(info));
    var version =
        bach()
            .get(Property.PROJECT_VERSION)
            .map(Version::parse)
            .orElseGet(() -> computeProjectVersion(info));
    var libraries = computeProjectLibraries(info);
    return new Project(name, version, libraries);
  }

  default ProjectInfo computeProjectInfo() {
    var info = getClass().getModule().getAnnotation(ProjectInfo.class);
    if (info != null) return info;
    return Bach.class.getModule().getAnnotation(ProjectInfo.class);
  }

  default String computeProjectName(ProjectInfo info) {
    var name = info.name();
    if (!name.equals("*")) return name;
    return "" + bach().base().directory().toAbsolutePath().getFileName();
  }

  default Version computeProjectVersion(ProjectInfo info) {
    return Version.parse(info.version());
  }

  default Libraries computeProjectLibraries(ProjectInfo info) {
    var requires = Set.of(info.requires());
    var lookups = new ArrayList<ModuleLookup>();
    for (var external : info.lookup()) lookups.add(computeProjectModuleLookup(external));
    return new Libraries(requires, List.copyOf(lookups));
  }

  default ModuleLookup computeProjectModuleLookup(ProjectInfo.External external) {
    var module = external.module();
    var target = external.via();
    return switch (external.type()) {
      case AUTO -> Libraries.lookup(module).via(target);
      case URI -> Libraries.lookup(module).viaUri(target);
      case PATH -> Libraries.lookup(module).viaPath(external.pathBase(), target);
      case MAVEN -> Libraries.lookup(module).viaMaven(external.mavenRepository(), target);
    };
  }

  default String computeMainJarFileName(String module) {
    return module + '@' + bach().project().toVersionNumberAndPreRelease() + ".jar";
  }

  default void buildProject() throws Exception {
    var bach = bach();
    var project = bach.project();
    bach.print("Build %s %s", project.name(), project.version());
    if (bach.is(Options.Flag.VERBOSE)) bach.info();
    var start = Instant.now();
    try {
      bach.loadMissingExternalModules();
      buildProjectMainSpace();
    } catch (Exception exception) {
      throw new RuntimeException("Build failed: " + exception);
    } finally {
      bach.print("Build took %s", Strings.toString(Duration.between(start, Instant.now())));
      var logbook = bach.writeLogbook();
      bach.print("Logbook written to %s", logbook.toUri());
    }
  }

  default void buildProjectMainSpace() throws Exception {}
}
