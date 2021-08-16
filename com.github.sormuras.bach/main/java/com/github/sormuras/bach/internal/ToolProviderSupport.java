package com.github.sormuras.bach.internal;

import java.nio.file.Path;
import java.util.spi.ToolProvider;

public sealed interface ToolProviderSupport permits ConstantInterface {
  /** {@return } */
  static String describe(ToolProvider provider) {
    var type = provider.getClass();
    var module = type.getModule();
    var service = type.getCanonicalName();
    if (module.isNamed()) return module.getDescriptor().toNameAndVersion() + "/" + service;
    var source = type.getProtectionDomain().getCodeSource();
    if (source != null) {
      var location = source.getLocation();
      if (location != null)
        try {
          return Path.of(location.toURI()).resolve(service).toUri().toString();
        } catch (Exception ignore) {
          // fall-through
        }
    }
    return module + "/" + service;
  }
}
