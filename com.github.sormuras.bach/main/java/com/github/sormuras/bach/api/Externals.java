package com.github.sormuras.bach.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record Externals(Set<String> requires, List<ExternalModuleLocator> locators) {

  public static Externals of(ExternalModuleLocator... locators) {
    return new Externals(Set.of(), List.of(locators));
  }

  public Optional<External> findExternal(String module) {
    for (var locator : locators) {
      var location = locator.locate(module);
      if (location.isEmpty()) continue;
      return Optional.of(new External(location.get(), locator));
    }
    return Optional.empty();
  }

  public record External(ExternalModuleLocation location, ExternalModuleLocator by) {}
}
