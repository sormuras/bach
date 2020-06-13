/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach.project;

import de.sormuras.bach.internal.Maven;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** A module-uri pair used to resolve external modules. */
public final class Locator implements Comparable<Locator> {

  /**
   * Create a new module locator pointing to an artifact hosted at Maven Central.
   *
   * @param module The module to used as the nominal part of the pair
   * @param group Maven Group ID
   * @param artifact Maven Artifact ID
   * @param version The version string
   * @return A new Maven Central-based {@code Locator} instance
   * @see <a href="https://search.maven.org">search.maven.org</a>
   */
  public static Locator ofCentral(String module, String group, String artifact, String version) {
    return new Locator(module, Maven.central(group, artifact, version));
  }

  /**
   * Create a new module locator pointing to an artifact hosted at Maven Central.
   *
   * @param module The module to used as the nominal part of the pair
   * @param gav Maven groupId + ':' + artifactId + ':' version [+ ':' + classifier]
   * @return A new Maven Central-based {@code Locator} instance
   * @see <a href="https://search.maven.org">search.maven.org</a>
   */
  public static Locator ofCentral(String module, String gav) {
    var split = gav.split(":");
    if (split.length < 3) throw new IllegalArgumentException();
    var joiner = new Maven.Joiner().group(split[0]).artifact(split[1]).version(split[2]);
    joiner.classifier(split.length < 4 ? "" : split[3]);
    return new Locator(module, joiner.toString());
  }

  /**
   * Create a new module locator pointing to an artifact built by JitPack.
   *
   * @param user GitHub username or the complete group like {@code "com.azure.${USER}"}
   * @param repository Name of the repository or project
   * @param version The version string of the repository or project, which is either a release tag,
   *     a commit hash, or {@code "${BRANCH}-SNAPSHOT"} for a version that has not been released.
   * @return A new JitPack-based {@code Locator} instance
   * @see <a href="https://jitpack.io/docs">jitpack.io</a>
   */
  public static Locator ofJitPack(String module, String user, String repository, String version) {
    var group = user.indexOf('.') == -1 ? "com.github." + user : user;
    var joiner = Maven.Joiner.of(group, repository, version);
    return new Locator(module, joiner.repository("https://jitpack.io").toString());
  }

  /**
   * Create a new locator pointing to a modular JUnit Jupiter JAR file hosted at Maven Central.
   *
   * @param suffix The suffix used to complete the module name and the Maven Artifact ID
   * @param version The version string
   * @return A new Maven Central-based {@code Locator} instance of JUnit Jupiter
   * @see <a
   *     href="https://search.maven.org/search?q=g:org.junit.jupiter">org.junit.platform[.]$suffix</a>
   */
  public static Locator ofJUnitJupiter(String suffix, String version) {
    var module = "org.junit.jupiter" + (suffix.isEmpty() ? "" : '.' + suffix);
    var artifact = "junit-jupiter" + (suffix.isEmpty() ? "" : '-' + suffix);
    return Locator.ofCentral(module, "org.junit.jupiter", artifact, version);
  }

  /**
   * Create a new locator pointing to a modular JUnit Platform JAR file hosted at Maven Central.
   *
   * @param suffix The suffix used to complete the module name and the Maven Artifact ID
   * @param version The version string
   * @return A new Maven Central-based {@code Locator} instance of JUnit Platform
   * @see <a
   *     href="https://search.maven.org/search?q=g:org.junit.platform">org.junit.platform.$suffix</a>
   */
  public static Locator ofJUnitPlatform(String suffix, String version) {
    var module = "org.junit.platform." + suffix;
    var artifact = "junit-platform-" + suffix;
    return Locator.ofCentral(module, "org.junit.platform", artifact, version);
  }

  private final String module;
  private final String uri;

  public Locator(String module, String uri) {
    this.module = module;
    this.uri = uri;
  }

  public String module() {
    return module;
  }

  public String uri() {
    return uri;
  }

  @Override
  public int compareTo(Locator other) {
    return module.compareTo(other.module);
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof Locator && module.equals(((Locator) object).module);
  }

  @Override
  public int hashCode() {
    return module.hashCode();
  }

  public List<String> findFragments(String key) {
    var list = new ArrayList<String>();
    var entries = URI.create(uri).getFragment().split("&");
    for (var entry : entries) if (entry.startsWith(key)) list.add(entry.substring(key.length()));
    return list;
  }

  public Map<String, String> findDigests() {
    var map = new TreeMap<String, String>();
    for (var pair : findFragments("digest-")) {
      int separator = pair.indexOf('=');
      if (separator <= 0) throw new IllegalStateException("Digest algorithm not found: " + pair);
      var algorithm = pair.substring(0, separator);
      var digest = pair.substring(pair.indexOf('=') + 1);
      map.put(algorithm, digest);
    }
    return map;
  }

  public Optional<Integer> findSize() {
    var versions = findFragments("size=");
    if (versions.size() == 0) return Optional.empty();
    if (versions.size() == 1) return Optional.of(Integer.valueOf(versions.get(0)));
    throw new IllegalStateException("Multiple versions found in fragment: " + versions);
  }

  public Optional<String> findVersion() {
    var versions = findFragments("version=");
    if (versions.size() == 0) return Optional.empty();
    if (versions.size() == 1) return Optional.of(versions.get(0));
    throw new IllegalStateException("Multiple versions found in fragment: " + versions);
  }

  public URI toURI() {
    return URI.create(uri);
  }

  public Locator withDigest(String algorithm, String digest) {
    var separator = uri.indexOf('#') == -1 ? '#' : '&';
    return new Locator(module, uri + separator + "digest-" + algorithm + '=' + digest);
  }

  public Locator withSize(long size) {
    var separator = uri.indexOf('#') == -1 ? '#' : '&';
    return new Locator(module, uri + separator + "size=" + size);
  }

  public Locator withVersion(String version) {
    var separator = uri.indexOf('#') == -1 ? '#' : '&';
    return new Locator(module, uri + separator + "version=" + version);
  }
}
