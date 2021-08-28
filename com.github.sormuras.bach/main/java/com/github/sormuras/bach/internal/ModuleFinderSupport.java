package com.github.sormuras.bach.internal;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Optional;
import java.util.function.Function;

/** Static utility methods for operating on instances of {@link ModuleFinder}. */
public sealed interface ModuleFinderSupport permits ConstantInterface {
  static Optional<String> findMainClass(ModuleFinder finder, String module) {
    return finder
        .find(module)
        .map(ModuleReference::descriptor)
        .map(ModuleDescriptor::mainClass)
        .flatMap(Function.identity());
  }
}
