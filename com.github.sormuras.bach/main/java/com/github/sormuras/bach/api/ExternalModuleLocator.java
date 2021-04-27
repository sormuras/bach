package com.github.sormuras.bach.api;

import java.util.Optional;

@FunctionalInterface
public interface ExternalModuleLocator {

  Optional<ExternalModuleLocation> locate(String module);

  default Stability stability() {
    return Stability.UNKNOWN;
  }

  default String title() {
    return getClass().getSimpleName();
  }

  enum Stability {
    UNKNOWN,
    DYNAMIC,
    STABLE
  }
}
