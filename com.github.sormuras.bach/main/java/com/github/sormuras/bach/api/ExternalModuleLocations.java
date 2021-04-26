package com.github.sormuras.bach.api;

import java.util.Map;
import java.util.Optional;

public record ExternalModuleLocations(Map<String, ExternalModuleLocation> locations)
    implements ExternalModuleLocator {

  @Override
  public Optional<ExternalModuleLocation> locate(String module) {
    return Optional.ofNullable(locations.get(module));
  }

  @Override
  public Stability stability() {
    return Stability.STABLE;
  }
}
