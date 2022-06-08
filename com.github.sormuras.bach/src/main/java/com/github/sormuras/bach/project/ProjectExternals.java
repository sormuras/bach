package com.github.sormuras.bach.project;

import java.util.Set;
import java.util.stream.Stream;

public record ProjectExternals(
    Set<String> requires, ExternalModuleLocators locators, Set<String> tools)
    implements ProjectComponent {

  public static ProjectExternals of() {
    return new ProjectExternals(Set.of(), ExternalModuleLocators.of(), Set.of());
  }

  public ProjectExternals withRequires(String... modules) {
    var requires = Set.copyOf(Stream.concat(requires().stream(), Stream.of(modules)).toList());
    return new ProjectExternals(requires, locators, tools);
  }

  public ProjectExternals with(ExternalModuleLocator... more) {
    return new ProjectExternals(requires, locators.with(more), tools);
  }

  public ProjectExternals withExternalTool(String... more) {
    var tools = Set.copyOf(Stream.concat(tools().stream(), Stream.of(more)).toList());
    return new ProjectExternals(requires, locators, tools);
  }
}
