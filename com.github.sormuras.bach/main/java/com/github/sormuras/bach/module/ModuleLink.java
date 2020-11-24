package com.github.sormuras.bach.module;

import com.github.sormuras.bach.internal.Maven;
import java.net.URI;
import java.util.Comparator;

/**
 * A module-uri pair used to connect a module name to a specific modular JAR file.
 *
 * <p>Examples:
 *
 * <pre>{@code
 * ModuleLink.module("junit").toUri("https://repo.maven.apache.org/maven2/junit/junit/4.13.1/junit-4.13.1.jar")
 * ModuleLink.module("junit").toMavenCentral("junit:junit:4.13.1")
 * }</pre>
 *
 * @param module the name of the module
 * @param uri the uniform resource identifier of the module
 */
public record ModuleLink(String module, String uri) implements Comparable<ModuleLink> {

  @Override
  public String toString() {
    return module + " -> " + uri;
  }

  @Override
  public int compareTo(ModuleLink o) {
    return Comparator.comparing(ModuleLink::module).thenComparing(ModuleLink::uri).compare(this, o);
  }

  /**
   * Returns a module link factory for the given module name.
   *
   * @param module the name of the module to link
   * @return a new module link factory
   */
  public static Factory link(String module) {
    return new Factory(module);
  }

  /** A builder of links. */
  public static class Factory {

    /** The name of the module to link. */
    private final String module;

    private Factory(String module) {
      this.module = module;
    }

    /**
     * Returns a module link whose uri is determined by the given target string.
     *
     * @param target the target to parse into a uri
     * @return a module link
     */
    public ModuleLink to(String target) {
      var maven = target.indexOf('/') == -1 && target.chars().filter(ch -> ch == ':').count() >= 2;
      if (maven) return toMavenCentral(target);
      return toUri(target);
    }

    /**
     * Returns a module link pointing to an artifact hosted at the given Maven repository.
     *
     * @param repository Maven repository
     * @param coordinates Maven groupId + ':' + artifactId + ':' version [+ ':' + classifier]
     * @return a Maven-based {@code Link} instance
     */
    public ModuleLink toMaven(String repository, String coordinates) {
      var split = coordinates.split(":");
      if (split.length < 3) throw new IllegalArgumentException();
      var version = split[2];
      var joiner = new Maven.Joiner().repository(repository);
      joiner.group(split[0]).artifact(split[1]).version(version);
      joiner.classifier(split.length < 4 ? "" : split[3]);
      return toUri(joiner.toString());
    }

    /**
     * Returns a module link pointing to an artifact hosted at Maven Central.
     *
     * @param coordinates Maven groupId + ':' + artifactId + ':' version [+ ':' + classifier]
     * @return a Maven Central-based {@code Link} instance
     * @see <a href="https://search.maven.org">search.maven.org</a>
     */
    public ModuleLink toMavenCentral(String coordinates) {
      return toMaven(Maven.CENTRAL_REPOSITORY, coordinates);
    }

    /**
     * Returns a module link pointing to the given uniform resource identifier.
     *
     * @param uri uniform resource identifier of the module
     * @return a module link
     */
    public ModuleLink toUri(String uri) {
      return new ModuleLink(module, URI.create(uri).toString());
    }
  }
}
