package com.github.sormuras.bach.api;

import java.util.Optional;

@FunctionalInterface
public interface ExternalModuleLocator {

  Optional<ExternalModuleLocation> locate(String module);

  default Stability stability() {
    return Stability.UNKNOWN;
  }

  enum Stability {
    UNKNOWN,
    DYNAMIC,
    STABLE
  }
}
