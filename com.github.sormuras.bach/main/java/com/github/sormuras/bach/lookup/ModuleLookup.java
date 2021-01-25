package com.github.sormuras.bach.lookup;

import java.util.Optional;

@FunctionalInterface
public interface ModuleLookup {
  Optional<String> lookupModule(String name);
}
