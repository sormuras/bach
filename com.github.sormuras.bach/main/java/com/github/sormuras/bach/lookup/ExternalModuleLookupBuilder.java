package com.github.sormuras.bach.lookup;

import java.net.URI;
import java.nio.file.Path;

/** A builder of external module objects. */
public class ExternalModuleLookupBuilder {

  /** The name of the module to link. */
  private final String module;

  /**
   * Constructs an instance of this class with the given components.
   *
   * @param module the name of the module to link
   */
  public ExternalModuleLookupBuilder(String module) {
    this.module = module;
  }

  /**
   * Returns a module link whose uri is determined by the given target string.
   *
   * @param target the target to parse into a uri
   * @return a module link
   */
  public ExternalModuleLookup to(String target) {
    var maven = target.indexOf('/') == -1 && target.chars().filter(ch -> ch == ':').count() >= 2;
    if (maven) return toMavenCentral(target);
    return toUri(target);
  }

  /**
   * Returns a module link pointing to local path.
   *
   * @param base the base directory
   * @param more the path to the file
   * @return an external module link
   */
  public ExternalModuleLookup toPath(String base, String more) {
    var first = base.equals("~") ? System.getProperty("user.home") : base;
    var path = Path.of(first, more);
    return toUri(path.normalize().toUri());
  }

  /**
   * Returns a module link pointing to an artifact hosted at the given Maven repository.
   *
   * @param repository Maven repository
   * @param coordinates Maven groupId + ':' + artifactId + ':' version [+ ':' + classifier]
   * @return a Maven-based {@code Link} instance
   */
  public ExternalModuleLookup toMaven(String repository, String coordinates) {
    var split = coordinates.split(":");
    if (split.length < 3) throw new IllegalArgumentException();
    var joiner = Maven.Joiner.of(split[0], split[1], split[2])
        .repository(repository)
        .classifier(split.length < 4 ? "" : split[3]);
    return toUri(joiner.toString());
  }

  /**
   * Returns a module link pointing to an artifact hosted at Maven Central.
   *
   * @param coordinates Maven groupId + ':' + artifactId + ':' version [+ ':' + classifier]
   * @return a Maven Central-based {@code Link} instance
   * @see <a href="https://search.maven.org">search.maven.org</a>
   */
  public ExternalModuleLookup toMavenCentral(String coordinates) {
    return toMaven(Maven.CENTRAL_REPOSITORY, coordinates);
  }

  /**
   * Returns a module link pointing to the given uniform resource identifier.
   *
   * @param uri uniform resource identifier of the module
   * @return a module link
   */
  public ExternalModuleLookup toUri(String uri) {
    return toUri(URI.create(uri));
  }

  /**
   * Returns a module link pointing to the given uniform resource identifier.
   *
   * @param uri uniform resource identifier of the module
   * @return a module link
   */
  public ExternalModuleLookup toUri(URI uri) {
    return new ExternalModuleLookup(module, uri.toString());
  }
}