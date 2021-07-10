package com.github.sormuras.bach.project;

import com.github.sormuras.bach.external.ExternalModuleLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public record ProjectExternals(Set<String> requires, List<ExternalModuleLocator> locators) {
  public ProjectExternals withRequires(String module, String... more) {
    var requires = new TreeSet<>(this.requires);
    requires.add(module);
    requires.addAll(Set.of(more));
    return new ProjectExternals(requires, locators);
  }

  public ProjectExternals with(ExternalModuleLocator locator, ExternalModuleLocator... more) {
    var locators = new ArrayList<>(this.locators);
    locators.add(locator);
    locators.addAll(List.of(more));
    return new ProjectExternals(requires, locators);
  }
}
