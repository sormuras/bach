package com.github.sormuras.bach.external;

import com.github.sormuras.bach.Bach;

import java.util.Optional;

@FunctionalInterface
public interface ExternalModuleLocator {

  Optional<ExternalModuleLocation> locate(String module);

  default Optional<ExternalModuleLocation> locate(Bach bach, String module) {
    return locate(module);
  }

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
