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

package de.sormuras.bach.tool;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A {@code jar} tool call configuration. */
public final class Jar implements Call<Jar> {

  /** Main operation mode. */
  public enum Mode {
    /** Create the archive. */
    CREATE,
    /** Extract named (or all) files from the archive. */
    EXTRACT,
    /** List the table of contents for the archive. */
    LIST,
    /** Update an existing jar archive. */
    UPDATE
  }

  /**
   * Create an empty jar call.
   *
   * @return An empty {@code Jar} instance.
   */
  public static Jar of() {
    return new Jar(List.of());
  }

  /**
   * Create a jar call with the given main operation mode.
   *
   * @param mode The main operation mode
   * @return An {@code Jar} instance with the given main operation mode.
   */
  public static Jar of(Mode mode) {
    return of().with("--" + mode.name().toLowerCase());
  }

  /**
   * Create a jar call in {@link Mode#CREATE CREATE} mode with the given archive file.
   *
   * @param file The path to the archive file
   * @return An {@code Jar} instance for creating the given archive file.
   */
  public static Jar of(Path file) {
    return of(Mode.CREATE).withArchive(file);
  }

  private final List<Argument> arguments;

  public Jar(List<Argument> arguments) {
    this.arguments = arguments;
  }

  @Override
  public String name() {
    return "jar";
  }

  @Override
  public List<Argument> arguments() {
    return arguments;
  }

  @Override
  public Jar with(List<Argument> arguments) {
    return new Jar(arguments);
  }

  /**
   * Create new jar instance with the given archive file name.
   *
   * @param file The archive file name
   * @return A new {@code Jar} instance with the archive file name
   */
  public Jar withArchive(Path file) {
    return with("--file", file);
  }

  /**
   * Change into the specified directory and includes the given files.
   *
   * @param directory The directory to change into
   * @param files The files to include
   * @return A new {@code Jar} instance with {@code -C} option appended
   */
  public Jar withChangeDirectoryAndIncludeFiles(Path directory, String... files) {
    var values = new ArrayList<String>();
    values.add(directory.toString());
    if (files.length > 0) Collections.addAll(values, files);
    var argument = new Argument("-C", values);
    return with(argument);
  }
}
