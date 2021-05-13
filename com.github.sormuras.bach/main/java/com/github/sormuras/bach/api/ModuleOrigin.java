package com.github.sormuras.bach.api;

import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;

public enum ModuleOrigin {
  /** Declared by the project. */
  DECLARED,
  /** Packaged in a modular JAR file. */
  EXTERNAL,
  /** Provided by the Java Runtime. */
  SYSTEM,
  /** Unknown origin. */
  UNKNOWN;

  public static ModuleOrigin of(Module module) {
    var configuration = module.getLayer().configuration();
    var reference = configuration.findModule(module.getName()).orElseThrow().reference();
    return ModuleOrigin.of(reference);
  }

  public static ModuleOrigin of(ModuleReference reference) {
    var location = reference.location();
    if (location.isEmpty()) return UNKNOWN;
    var uri = location.get();
    if (uri.getScheme().equals("jrt")) return SYSTEM;
    var string = uri.toString();
    if (string.endsWith(".java")) return DECLARED;
    if (string.endsWith(".jar") || Files.isDirectory(Path.of(uri))) return EXTERNAL;
    return UNKNOWN;
  }
}
