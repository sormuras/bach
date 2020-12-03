package com.github.sormuras.bach.project;

import java.util.Comparator;
import java.util.Optional;

/**
 * A module-uri pair used to connect a module name to a specific modular JAR file.
 *
 * <p>Examples:
 *
 * <pre>{@code
 * ExternalModule.link("junit").toUri("https://repo.maven.apache.org/maven2/junit/junit/4.13.1/junit-4.13.1.jar")
 * ExternalModule.link("junit").toMavenCentral("junit:junit:4.13.1")
 * }</pre>
 *
 * @param module the name of the module
 * @param uri the uniform resource identifier of the module
 */
public record ExternalModule(String module, String uri)
    implements Comparable<ExternalModule>, ModuleLookup {

  @Override
  public int compareTo(ExternalModule o) {
    return Comparator.comparing(ExternalModule::module)
        .thenComparing(ExternalModule::uri)
        .compare(this, o);
  }

  @Override
  public Optional<String> lookup(String name) {
    return module.equals(name) ? Optional.of(uri) : Optional.empty();
  }

  @Override
  public String toString() {
    return module + " -> " + uri;
  }

  /**
   * Returns an external module builder for the given module name.
   *
   * @param module the name of the module to link
   * @return a external module builder
   */
  public static ExternalModuleBuilder link(String module) {
    return new ExternalModuleBuilder(module);
  }
}
