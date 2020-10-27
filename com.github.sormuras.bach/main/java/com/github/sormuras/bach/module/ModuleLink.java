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
 */
public final class ModuleLink implements Comparator<ModuleLink> {

  /** The name of the module. */
  private final String module;

  /** The uniform resource identifier of the module. */
  private final String uri;

  /**
   * Initialize a link with the given components.
   *
   * @param module the name of the module
   * @param uri the URI of the modular JAR file
   */
  public ModuleLink(String module, String uri) {
    this.module = module;
    this.uri = uri;
  }

  /**
   * Returns the name of the module.
   *
   * @return the name of the module
   */
  public String module() {
    return module;
  }

  /**
   * Returns the URI of the module.
   *
   * @return the URI of the module
   */
  public String uri() {
    return uri;
  }

  @Override
  public String toString() {
    return module + " -> " + uri;
  }

  @Override
  public int compare(ModuleLink o1, ModuleLink o2) {
    return o1.module.compareTo(o2.module);
  }

  /**
   * Returns a module link factory for the given module name.
   *
   * @param module the name of the module to link
   * @return a new module link factory
   */
  public static Factory module(String module) {
    return new Factory(module);
  }

  /**
   * Returns a module link pointing to the URI specified in the given annotation.
   *
   * @param link the annotation to parse
   * @return a module link based on the given annotation
   */
  public static ModuleLink of(Link link) {
    return module(link.module()).toUri(link.uri());
  }

  /**
   * Returns a module link pointing to a modular JUnit Jupiter JAR file hosted at Maven Central.
   *
   * @param suffix the suffix used to complete the module name and the Maven Artifact ID
   * @param version the version string
   * @return a new Maven Central-based {@code Link} instance of JUnit Jupiter
   * @see <a
   *     href="https://search.maven.org/search?q=g:org.junit.jupiter">org.junit.jupiter[.]$suffix</a>
   */
  public static ModuleLink ofJUnitJupiter(String suffix, String version) {
    var module = "org.junit.jupiter" + (suffix.isEmpty() ? "" : '.' + suffix);
    var artifact = "junit-jupiter" + (suffix.isEmpty() ? "" : '-' + suffix);
    return module(module).toMavenCentral("org.junit.jupiter:" + artifact + ':' + version);
  }

  /**
   * Returns a module link pointing to a modular JUnit Platform JAR file hosted at Maven Central.
   *
   * @param suffix the suffix used to complete the module name and the Maven Artifact ID
   * @param version the version string
   * @return a new Maven Central-based {@code Link} instance of JUnit Platform
   * @see <a
   *     href="https://search.maven.org/search?q=g:org.junit.platform">org.junit.platform.$suffix</a>
   */
  public static ModuleLink ofJUnitPlatform(String suffix, String version) {
    var module = "org.junit.platform." + suffix;
    var artifact = "junit-platform-" + suffix;
    return module(module).toMavenCentral("org.junit.platform:" + artifact + ':' + version);
  }

  /** A builder of links. */
  public static class Factory {

    /** The name of the module to link. */
    private final String module;

    private Factory(String module) {
      this.module = module;
    }

    /**
     * Returns a module link pointing to an artifact hosted at Maven Central.
     *
     * @param coordinates Maven groupId + ':' + artifactId + ':' version [+ ':' + classifier]
     * @return a Maven Central-based {@code Link} instance
     * @see <a href="https://search.maven.org">search.maven.org</a>
     */
    public ModuleLink toMavenCentral(String coordinates) {
      var split = coordinates.split(":");
      if (split.length < 3) throw new IllegalArgumentException();
      var version = split[2];
      var joiner = new Maven.Joiner().group(split[0]).artifact(split[1]).version(version);
      joiner.classifier(split.length < 4 ? "" : split[3]);
      return toUri(joiner.toString());
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
