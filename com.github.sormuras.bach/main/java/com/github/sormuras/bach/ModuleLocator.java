package com.github.sormuras.bach;

import java.util.Optional;

@FunctionalInterface
public interface ModuleLocator {

  String locate(String module);

  default Optional<String> find(String module) {
    return Optional.ofNullable(locate(module));
  }

  default String caption() {
    return getClass().getSimpleName();
  }
}
