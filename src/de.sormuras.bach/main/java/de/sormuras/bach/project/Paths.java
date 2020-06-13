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

import java.nio.file.Path;

/**
 * A collection of well-known directories and derived paths.
 *
 * <h3>Example</h3>
 *
 * <pre><code>
 * Component     Directory Tree Example
 * --------------------------------------------------------
 * base -------> jigsaw-quick-start
 *               ├───.bach
 *               │   │   build.jsh
 *               │   ├───lib
 *               │   │       de.sormuras.bach@11.3.jar
 *               │   ├───src
 * workspace ----│-> └───workspace
 *               │       ├───classes
 *               │       │   └───11
 *               │       │       ├───com.greetings
 *               │       │       │       module-info.class
 *               │       │       └───com
 *               │       │           └───greetings
 *               │       │                   Main.class
 *               │       ├───classes-test
 *               │       ├───documentation
 *               │       ├───image
 *               │       ├───modules
 *               │       │       com.greetings@1-ea.jar
 *               │       ├───modules-test
 *               │       ├───reports
 *               │       └───sources
 * library ----> ├───lib
 *               └───src
 *                   └───com.greetings
 *                       │   module-info.java
 *                       └───com
 *                           └───greetings
 *                                   Main.java
 *             </code></pre>
 */
public final class Paths {

  /**
   * Return a new {@code Paths} instance for the current user directory.
   *
   * <ul>
   *   <li>{@code base} = {@code Path.of("")}
   *   <li>{@code library} = {@code Path.of("lib")}
   *   <li>{@code workspace} = {@code Path.of(".bach", "workspace")}
   * </ul>
   *
   * @return A new default paths object
   */
  public static Paths of() {
    return of(Path.of(""));
  }

  /**
   * Return a new {@code Paths} instance for the given base directory.
   *
   * <ul>
   *   <li>{@code base} = {@code Path.of("project")}
   *   <li>{@code library} = {@code Path.of("project", "lib")}
   *   <li>{@code workspace} = {@code Path.of("project", ".bach", "workspace")}
   * </ul>
   *
   * @param directory The path to used as the base directory
   * @return A new paths object initialized for the given base directory
   */
  public static Paths of(Path directory) {
    var base = directory.normalize();
    return new Paths(base, base.resolve("lib"), base.resolve(".bach/workspace"));
  }

  private final Path base;
  private final Path library;
  private final Path workspace;

  /**
   * Initializes a new {@code Paths} instance with the given component values.
   *
   * @param base The path to the root directory of the project
   * @param library The directory that contains 3rd-party modules
   * @param workspace The directory that collects all generated assets
   */
  public Paths(Path base, Path library, Path workspace) {
    this.base = base;
    this.library = library;
    this.workspace = workspace;
  }

  public Path base() {
    return base;
  }

  public Path library() {
    return library;
  }

  public Path workspace() {
    return workspace;
  }

  public Path base(String entry, String... more) {
    return base.resolve(Path.of(entry, more));
  }

  public Path library(String entry) {
    return library.resolve(entry);
  }

  public Path workspace(String entry, String... more) {
    return workspace.resolve(Path.of(entry, more));
  }

  public Path classes(String realm, int release) {
    return workspace("classes" + realm(realm), String.valueOf(release));
  }

  public Path classes(String realm, int release, String module) {
    return classes(realm, release).resolve(module);
  }

  public Path modules(String realm) {
    return workspace("modules" + realm(realm));
  }

  public Path sources(String realm) {
    return workspace("sources", realm);
  }

  static String realm(String name) {
    return name.isEmpty() ? "" : "-" + name;
  }
}
