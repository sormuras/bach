package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.Maven;
import java.net.URI;
import java.nio.file.Path;

/** A builder of external module objects. */
public class ExternalModuleBuilder {

  /** The name of the module to link. */
  private final String module;

  /**
   * Constructs an instance of this class with the given components.
   *
   * @param module the name of the module to link
   */
  public ExternalModuleBuilder(String module) {
    this.module = module;
  }

  /**
   * Returns a module link whose uri is determined by the given target string.
   *
   * @param target the target to parse into a uri
   * @return a module link
   */
  public ExternalModule to(String target) {
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
  public ExternalModule toPath(String base, String more) {
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
  public ExternalModule toMaven(String repository, String coordinates) {
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
  public ExternalModule toMavenCentral(String coordinates) {
    return toMaven(Maven.CENTRAL_REPOSITORY, coordinates);
  }

  /**
   * Returns a module link pointing to the given uniform resource identifier.
   *
   * @param uri uniform resource identifier of the module
   * @return a module link
   */
  public ExternalModule toUri(String uri) {
    return toUri(URI.create(uri));
  }

  /**
   * Returns a module link pointing to the given uniform resource identifier.
   *
   * @param uri uniform resource identifier of the module
   * @return a module link
   */
  public ExternalModule toUri(URI uri) {
    return new ExternalModule(module, uri.toString());
  }
}
