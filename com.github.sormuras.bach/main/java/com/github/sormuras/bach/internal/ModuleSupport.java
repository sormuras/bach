package com.github.sormuras.bach.internal;

/** Static utility methods for operating on instances of {@link Module}. */
public sealed interface ModuleSupport permits ConstantInterface {

  static String version(Module module) {
    if (!module.isNamed()) return "(unnamed)";
    return module.getDescriptor().version().map(Object::toString).orElse("(unknown)");
  }
}
