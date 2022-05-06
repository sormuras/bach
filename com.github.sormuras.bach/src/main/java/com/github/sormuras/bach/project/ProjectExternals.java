package com.github.sormuras.bach.project;

import java.util.Set;
import java.util.stream.Stream;

public record ProjectExternals(
    Set<String> requires, ExternalModuleLocators locators, ExternalTools tools)
    implements Project.Component {

  public static ProjectExternals of() {
    return new ProjectExternals(Set.of(), ExternalModuleLocators.of(), ExternalTools.of());
  }

  public ProjectExternals withRequires(String... modules) {
    var requires = Set.copyOf(Stream.concat(requires().stream(), Stream.of(modules)).toList());
    return new ProjectExternals(requires, locators, tools);
  }

  public ProjectExternals with(ExternalModuleLocator... more) {
    return new ProjectExternals(requires, locators.with(more), tools);
  }

  public ProjectExternals with(ExternalTool... more) {
    return new ProjectExternals(requires, locators, tools.with(more));
  }
}
