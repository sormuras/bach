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

import java.lang.module.ModuleDescriptor.Version;
import java.util.function.UnaryOperator;

/** A modular Java project descriptor. */
public final class Project {

  /**
   * Return a new {@code Project} instance with default component values.
   *
   * <ul>
   *   <li>{@code version} = {@code "1-ea"}
   *   <li>{@code library} = An empty library
   * </ul>
   *
   * @return A new default project object
   */
  public static Project of() {
    return new Project(Version.parse("1-ea"), Library.of());
  }

  private final Version version;
  private final Library library;

  /**
   * Initializes a new project instance with the given component values.
   *
   * @param version The version of this project
   * @param library The library of this project
   */
  public Project(Version version, Library library) {
    this.version = version;
    this.library = library;
  }

  /**
   * Return the {@code Version} object representing the project's version.
   *
   * @return A {@code Version} object
   */
  public Version version() {
    return version;
  }

  /**
   * Return the {@code Library} object used by this project.
   *
   * @return A {@code Library} object
   */
  public Library library() {
    return library;
  }

  /**
   * Apply an operation based on the this {@code Project} instance.
   *
   * @param operator The operator to apply
   * @return A new {@code Project} instance with the operation applied or the same {@code Project}
   *     instance indicating no change was applied by the operator
   * @see UnaryOperator#identity()
   */
  public Project apply(UnaryOperator<Project> operator) {
    return operator.apply(this);
  }

  /**
   * Set the version of this project.
   *
   * @param version The string-representation of the version to set
   * @return A new {@code Project} instance with given version
   * @see Version#parse(String)
   */
  public Project version(String version) {
    return version(Version.parse(version));
  }

  /**
   * Set the version of this project.
   *
   * @param version The version to set
   * @return A new {@code Project} instance with given version
   */
  public Project version(Version version) {
    return new Project(version, library());
  }

  /**
   * Set the library of this project.
   *
   * @param library The library to set
   * @return A new {@code Project} instance with given library
   */
  public Project library(Library library) {
    return new Project(version(), library);
  }
}
