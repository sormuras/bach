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
 * A collection of project-defining directories and derived paths.
 *
 * <h2>Example</h2>
 *
 * <pre><code>
 * Component     Directory Tree Example
 * --------------------------------------------------------
 * directory --- jigsaw-quick-start
 *               ├───.bach
 *               │   │   build.jsh
 *               │   ├───lib
 *               │   │       de.sormuras.bach@11.3.jar
 *               │   ├───src
 * workspace ----│-- └───workspace
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
 * libraries --- ├───lib
 *               └───src
 *                   └───com.greetings
 *                       │   module-info.java
 *                       └───com
 *                           └───greetings
 *                                   Main.java
 *             </code></pre>
 */
public final class Base {

  /**
   * Return a new {@code Base} instance for the current user directory.
   *
   * <ul>
   *   <li>{@code directory} = {@code Path.of("")}
   *   <li>{@code libraries} = {@code Path.of("lib")}
   *   <li>{@code workspace} = {@code Path.of(".bach", "workspace")}
   * </ul>
   *
   * @return A new default base object
   */
  public static Base of() {
    return of("");
  }

  /**
   * Return a new {@code Base} instance for the given directory.
   *
   * <ul>
   *   <li>{@code directory} = {@code Path.of(first, more)}
   *   <li>{@code libraries} = {@code Path.of(first, more, "lib")}
   *   <li>{@code workspace} = {@code Path.of(first, more, ".bach", "workspace")}
   * </ul>
   *
   * @param first The first path element to use as the base directory
   * @param more The array of path elements to complete the base directory
   * @return A new base object initialized for the given directory
   */
  public static Base of(String first, String... more) {
    return of(Path.of(first, more));
  }

  /**
   * Return a new {@code Base} instance for the given directory.
   *
   * <ul>
   *   <li>{@code directory} = {@code directory}
   *   <li>{@code libraries} = {@code directory.resolve("lib")}
   *   <li>{@code workspace} = {@code directory.resolve(".bach", "workspace")}
   * </ul>
   *
   * @param directory The path to use as the base directory
   * @return A new base object initialized for the given directory
   */
  public static Base of(Path directory) {
    var base = directory.normalize();
    return new Base(base, base.resolve("lib"), base.resolve(".bach/workspace"));
  }

  private final Path directory;
  private final Path libraries;
  private final Path workspace;

  /**
   * Initializes a new {@code Base} instance with the given component values.
   *
   * @param directory The path to the base directory of the project
   * @param libraries The directory that contains 3rd-party modules
   * @param workspace The directory that collects all generated assets
   */
  public Base(Path directory, Path libraries, Path workspace) {
    this.directory = directory;
    this.libraries = libraries;
    this.workspace = workspace;
  }

  public Path directory() {
    return directory;
  }

  public Path libraries() {
    return libraries;
  }

  public Path workspace() {
    return workspace;
  }

  public Path directory(String entry, String... more) {
    return directory.resolve(Path.of(entry, more));
  }

  public Path libraries(String entry) {
    return libraries.resolve(entry);
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
