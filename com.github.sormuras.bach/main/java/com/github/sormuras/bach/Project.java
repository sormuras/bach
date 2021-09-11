package com.github.sormuras.bach;

import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ProjectExternals;
import com.github.sormuras.bach.project.ProjectName;
import com.github.sormuras.bach.project.ProjectSpace;
import com.github.sormuras.bach.project.ProjectSpaces;
import com.github.sormuras.bach.project.ProjectVersion;
import java.lang.module.ModuleDescriptor;
import java.util.List;
import java.util.Set;

public record Project(
    ProjectName name, ProjectVersion version, ProjectSpaces spaces, ProjectExternals externals) {

  public ProjectSpace space(String name) {
    return spaces.find(name).orElseThrow();
  }

  public String toNameAndVersion() {
    return "%s %s".formatted(name.value(), version.value());
  }

  public static Project of(String name, String version) {
    return new Project(
        new ProjectName(name),
        new ProjectVersion(ModuleDescriptor.Version.parse(version)),
        new ProjectSpaces(List.of()),
        new ProjectExternals(Set.of(), ExternalModuleLocators.of()));
  }

  public Project with(Object component) {
    assert component.getClass().isRecord(); // and is handled by exactly one of the following cases
    return new Project(
        component instanceof ProjectName name ? name : name,
        component instanceof ProjectVersion version ? version : version,
        component instanceof ProjectSpaces spaces ? spaces : spaces,
        component instanceof ProjectExternals externals ? externals : externals);
  }

  public Project with(Options options) {
    return with(options.forProject());
  }

  public Project with(Options.ProjectOptions options) {
    var project = this;
    if (options.name().isPresent()) project = project.withName(options.name().get());
    if (options.version().isPresent()) project = project.withVersion(options.version().get());
    return project;
  }

  public Project withName(String name) {
    return with(new ProjectName(name));
  }

  public Project withVersion(String version) {
    return withVersion(ModuleDescriptor.Version.parse(version));
  }

  public Project withVersion(ModuleDescriptor.Version version) {
    return with(new ProjectVersion(version));
  }

  public Project withSpaces(ProjectSpaces.Operator operator) {
    return with(operator.apply(spaces));
  }

  public Project withExternals(ProjectExternals.Operator operator) {
    return with(operator.apply(externals));
  }

  public Project withModuleTweak(
      String spaceName, String moduleName, DeclaredModule.Operator operator) {
    var space = space(spaceName);
    var module = space.module(moduleName);
    return with(spaces.tweakModule(new DeclaredModule.Tweak(space, module, operator)));
  }
}
