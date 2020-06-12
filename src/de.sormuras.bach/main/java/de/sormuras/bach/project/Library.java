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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.UnaryOperator;

/** A library maps module names to uris and declares additional module requirements. */
public final class Library {

  /**
   * Return a {@code Library} instance with default component values.
   *
   * @return A new library
   */
  public static Library of() {
    return new Library(Set.of(), Map.of());
  }

  private final Set<String> requires;
  private final Map<String, String> uris;

  /**
   * Initializes a new library with the given component values.
   *
   * @param requires The module dependences of this library
   * @param uris The module name to URI string mappings
   */
  public Library(Set<String> requires, Map<String, String> uris) {
    this.requires = Set.copyOf(requires);
    this.uris = Map.copyOf(uris);
  }

  /**
   * Return the set of {@code String} objects representing the module dependences.
   *
   * @return A possibly-empty unmodifiable set of {@code String} objects
   */
  public Set<String> requires() {
    return requires;
  }

  /**
   * Return the map of {@code String} objects representing the module names to URI mappings.
   *
   * @return A possibly-empty unmodifiable map of {@code String} objects
   * @see #findUri(String)
   */
  public Map<String, String> uris() {
    return uris;
  }

  /**
   * Return the {@code URI} mapped to module or an empty {@code Optional}.
   *
   * @return A possibly-empty optional containing the mapped {@link URI} object
   * @see #uris()
   * @see #map(String, String)
   */
  public Optional<URI> findUri(String module) {
    return Optional.ofNullable(uris().get(module)).map(URI::create);
  }

  /**
   * Add a dependence on a module and on an array of modules.
   *
   * @param module The module name to add
   * @param modules An possible empty array of more module name to add
   * @return A new {@code Library} instance with all modules added to the {@link #requires()} set
   */
  public Library requires(String module, String... modules) {
    var set = new TreeSet<>(requires());
    set.add(module);
    if (modules.length > 0) Collections.addAll(set, modules);
    return new Library(set, Map.of());
  }

  /**
   * Map a module to a string-representation of a URI.
   *
   * @param module The name of the module to map
   * @param uri The string-representation of the URI
   * @return A new {@code Library} instance with the new mapping
   * @throws IllegalArgumentException If the given uri string violates {@code RFC 2396}
   * @see #map(String)
   */
  public Library map(String module, String uri) {
    URI.create(uri);
    var map = new TreeMap<>(uris());
    map.put(module, uri);
    return new Library(requires, map);
  }

  /**
   * Add all module to string-representation of a URI mappings.
   *
   * @param moduleToUriMap The map to add
   * @return A new {@code Library} instance with the new mappings
   * @throws IllegalArgumentException If a given uri string violates {@code RFC 2396}
   * @see #map(String)
   */
  public Library map(Map<String, String> moduleToUriMap) {
    moduleToUriMap.values().forEach(URI::create);
    var map = new TreeMap<>(uris());
    map.putAll(moduleToUriMap);
    return new Library(requires, map);
  }

  /**
   * Apply an operation based on the this {@code Library} instance.
   *
   * @param operator The operator to apply
   * @return A new {@code Library} instance with the operation applied or the same {@code Library}
   *     instance indicating no change was applied by the operator
   * @see UnaryOperator#identity()
   */
  public Library apply(UnaryOperator<Library> operator) {
    return operator.apply(this);
  }

  /**
   * Map a module to a string-representation of a URI.
   *
   * @param module The name of the module to map
   * @return A new {@code ModuleMapper} instance to operate on
   * @see ModuleMapper#toJitPack(String, String, String)
   * @see ModuleMapper#toMavenCentral(String, String, String)
   */
  public ModuleMapper map(String module) {
    return new ModuleMapper(module);
  }

  /** A module name to string-representation of a URI mapping helper. */
  public class ModuleMapper {
    /** The module to map */
    private final String module;

    /**
     * Map a module to a JitPack-based URI string.
     *
     * @param module The name of the module to map
     */
    private ModuleMapper(String module) {
      this.module = module;
    }

    /**
     * Map a module to a string-representation of a URI.
     *
     * @param uri The string-representation of the URI
     * @return A new {@code Library} instance with the new mapping
     * @see #toJitPack(String, String, String)
     * @see #toMavenCentral(String, String, String)
     */
    public Library to(String uri) {
      return map(module, uri);
    }

    /**
     * Map a module to a JitPack-based URI string.
     *
     * @param user GitHub username or the Maven Group ID like {@code "com.azure.${USER}"}
     * @param repository Name of the repository or project
     * @param version The version string of the repository or project, which is either a release
     *     tag, a commit hash, or {@code "${BRANCH}-SNAPSHOT"} for a version that has not been
     *     released.
     * @return A new {@code Library} instance with the new JitPack-based mapping
     * @see <a href="https://jitpack.io/docs">jitpack.io</a>
     */
    public Library toJitPack(String user, String repository, String version) {
      var group = user.indexOf('.') == -1 ? "com.github." + user : user;
      var joiner = Maven.Joiner.of(group, repository, version);
      return map(module, joiner.repository("https://jitpack.io").toString());
    }

    /**
     * Map a module to an artifact deployed to Maven Central.
     *
     * @param group Maven Group ID
     * @param artifact Maven Artifact ID
     * @param version The version string
     * @return A new {@code Library} instance with the new Maven Central-based mapping
     * @see <a href="https://search.maven.org">search.maven.org</a>
     */
    public Library toMavenCentral(String group, String artifact, String version) {
      return map(module, Maven.central(group, artifact, version));
    }
  }
}
