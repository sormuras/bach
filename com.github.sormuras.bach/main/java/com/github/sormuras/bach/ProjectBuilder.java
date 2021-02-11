package com.github.sormuras.bach;

import static com.github.sormuras.bach.Options.Property.PROJECT_NAME;
import static com.github.sormuras.bach.Options.Property.PROJECT_VERSION;

import com.github.sormuras.bach.lookup.ModuleLookup;
import java.lang.module.ModuleDescriptor.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** A builder of project instances. */
public class ProjectBuilder {

  private final Bach bach;
  private final ProjectInfo info;

  public ProjectBuilder(Bach bach) {
    this.bach = bach;
    this.info = bach.computeProjectInfo();
  }

  public Project build() {
    return new Project()
        .name(bach.get(PROJECT_NAME).orElseGet(this::computeName))
        .version(bach.get(PROJECT_VERSION).map(Version::parse).orElseGet(this::computeVersion))
        .libraries(computeLibraries());
  }

  public String computeName() {
    var name = info.name();
    if (!name.equals("*")) return name;
    return "" + bach.base().directory().toAbsolutePath().getFileName();
  }

  public Version computeVersion() {
    return Version.parse(info.version());
  }

  public Libraries computeLibraries() {
    var requires = Set.of(info.requires());
    var lookups = new ArrayList<ModuleLookup>();
    for (var external : info.lookup()) lookups.add(computeModuleLookup(external));
    return new Libraries(requires, List.copyOf(lookups));
  }

  public ModuleLookup computeModuleLookup(ProjectInfo.External external) {
    var module = external.module();
    var target = external.via();
    return switch (external.type()) {
      case AUTO -> Libraries.lookup(module).via(target);
      case URI -> Libraries.lookup(module).viaUri(target);
      case PATH -> Libraries.lookup(module).viaPath(external.pathBase(), target);
      case MAVEN -> Libraries.lookup(module).viaMaven(external.mavenRepository(), target);
    };
  }
}
