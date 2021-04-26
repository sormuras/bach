package com.github.sormuras.bach.api;

import java.util.Optional;

public record ExternalModuleLocation(String module, String uri) implements ExternalModuleLocator {

  @Override
  public Optional<ExternalModuleLocation> locate(String module) {
    return this.module.equals(module) ? Optional.of(this) : Optional.empty();
  }

  @Override
  public Stability stability() {
    return Stability.STABLE;
  }
}
