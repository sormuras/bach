package com.github.sormuras.bach;

import java.util.Optional;

/** An external module locator tries to link a module name to a remote location. */
@FunctionalInterface
public interface ExternalModuleLocator {

  String locate(String module);

  default Optional<String> find(String module) {
    return Optional.ofNullable(locate(module));
  }

  default String caption() {
    return getClass().getSimpleName();
  }
}
