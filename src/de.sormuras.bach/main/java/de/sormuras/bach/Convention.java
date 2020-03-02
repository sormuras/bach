package de.sormuras.bach;

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Common conventions.
 *
 * @see <a href="https://github.com/sormuras/bach#common-conventions">Common Conventions</a>
 */
interface Convention {

  /** Return name of main class of the specified module. */
  static Optional<String> mainClass(Path info, String module) {
    var main = Path.of(module.replace('.', '/'), "Main.java");
    var exists = Files.isRegularFile(info.resolveSibling(main));
    return exists ? Optional.of(module + '.' + "Main") : Optional.empty();
  }

  /** Extend the passed map of modules with missing JUnit test engine implementations. */
  static void amendJUnitTestEngines(Map<String, Version> modules) {
    var names = modules.keySet();
    if (names.contains("org.junit.jupiter") || names.contains("org.junit.jupiter.api"))
      modules.putIfAbsent("org.junit.jupiter.engine", null);
    if (names.contains("junit")) modules.putIfAbsent("org.junit.vintage.engine", null);
  }

  /** Extend the passed map of modules with the JUnit Platform Console module. */
  static void amendJUnitPlatformConsole(Map<String, Version> modules) {
    var names = modules.keySet();
    if (names.contains("org.junit.platform.console")) return;
    var triggers =
        Set.of("org.junit.jupiter.engine", "org.junit.vintage.engine", "org.junit.platform.engine");
    names.stream()
        .filter(triggers::contains)
        .findAny()
        .ifPresent(__ -> modules.put("org.junit.platform.console", null));
  }
}
