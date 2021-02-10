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
    for (var lookup : info.lookups()) lookups.add(computeModuleLookup(lookup));
    return new Libraries(requires, List.copyOf(lookups));
  }

  public ModuleLookup computeModuleLookup(ProjectInfo.Lookup lookup) {
    var module = lookup.module();
    var target = lookup.via();
    return switch (lookup.type()) {
      case AUTO -> Libraries.link(module).to(target);
      case URI -> Libraries.link(module).toUri(target);
      case PATH -> Libraries.link(module).toPath(lookup.pathBase(), target);
      case MAVEN -> Libraries.link(module).toMaven(lookup.mavenRepository(), target);
    };
  }
}
