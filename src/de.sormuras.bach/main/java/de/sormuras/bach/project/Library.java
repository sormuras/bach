package de.sormuras.bach.project;

import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/** Manage external 3rd-party modules. */
public /*record*/ class Library {

  public enum Modifier {
    ADD_MISSING_JUNIT_TEST_ENGINES,
    ADD_MISSING_JUNIT_PLATFORM_CONSOLE
  }

  public static Library of() {
    return new Library(
        EnumSet.allOf(Modifier.class),
        module -> null,
        Library::defaultRepository,
        module -> null,
        module -> null);
  }

  public static String defaultRepository(String group, String version) {
    return version.endsWith("SNAPSHOT")
        ? "https://oss.sonatype.org/content/repositories/snapshots"
        : "https://repo1.maven.org/maven2";
  }

  public static void addJUnitTestEngines(Map<String, Set<Version>> map) {
    if (map.containsKey("org.junit.jupiter") || map.containsKey("org.junit.jupiter.api")) {
      map.putIfAbsent("org.junit.jupiter.engine", Set.of());
    }
    if (map.containsKey("junit")) {
      map.putIfAbsent("org.junit.vintage", Set.of());
    }
  }

  public static void addJUnitPlatformConsole(Map<String, Set<Version>> map) {
    if (map.containsKey("org.junit.jupiter.engine") || map.containsKey("org.junit.vintage")) {
      map.putIfAbsent("org.junit.platform.console", Set.of());
    }
  }

  private final Set<Modifier> modifiers;
  /** Map external 3rd-party module names to their {@code URI}s. */
  private final Function<String, URI> moduleMapper;
  /** Map Maven group ID and version to their Maven repository. */
  private final BinaryOperator<String> mavenRepositoryMapper;
  /** Map external 3rd-party module names to their colon-separated Maven Group and Artifact ID. */
  private final UnaryOperator<String> mavenGroupColonArtifactMapper;
  /** Map external 3rd-party module names to their Maven version. */
  private final UnaryOperator<String> mavenVersionMapper;

  Library(
      Set<Modifier> modifiers,
      Function<String, URI> moduleMapper,
      BinaryOperator<String> mavenRepositoryMapper,
      UnaryOperator<String> mavenGroupColonArtifactMapper,
      UnaryOperator<String> mavenVersionMapper) {
    this.modifiers = modifiers.isEmpty() ? Set.of() : EnumSet.copyOf(modifiers);
    this.moduleMapper = moduleMapper;
    this.mavenRepositoryMapper = mavenRepositoryMapper;
    this.mavenGroupColonArtifactMapper = mavenGroupColonArtifactMapper;
    this.mavenVersionMapper = mavenVersionMapper;
  }

  public Set<Modifier> modifiers() {
    return modifiers;
  }

  // TODO NOT USED?!
  public Function<String, URI> moduleMapper() {
    return moduleMapper;
  }

  public BinaryOperator<String> mavenRepositoryMapper() {
    return mavenRepositoryMapper;
  }

  public UnaryOperator<String> mavenGroupColonArtifactMapper() {
    return mavenGroupColonArtifactMapper;
  }

  public UnaryOperator<String> mavenVersionMapper() {
    return mavenVersionMapper;
  }

  public boolean addMissingJUnitTestEngines() {
    return modifiers.contains(Modifier.ADD_MISSING_JUNIT_TEST_ENGINES);
  }

  public boolean addMissingJUnitPlatformConsole() {
    return modifiers.contains(Modifier.ADD_MISSING_JUNIT_PLATFORM_CONSOLE);
  }
}
