package com.github.sormuras.bach.api;

import java.util.List;
import java.util.Set;

public record Externals(Set<String> requires, List<ExternalModuleLocator> locators) {

  public static Externals of(ExternalModuleLocator... locators) {
    return new Externals(Set.of(), List.of(locators));
  }
}
