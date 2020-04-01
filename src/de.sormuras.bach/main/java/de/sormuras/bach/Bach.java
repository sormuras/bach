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

package de.sormuras.bach;

import de.sormuras.bach.api.Project;

import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.util.Objects;
import java.util.function.Consumer;

/** Bach - Java Shell Builder. */
public class Bach {

  /** Version of the Java Shell Builder. */
  public static final Version VERSION = Version.parse("11.0-ea");

  /** Main entry-point. */
  public static void main(String... args) {
    Main.main(args);
  }

  /** Line-based message printing consumer. */
  private final Consumer<String> printer;

  /** Verbosity flag. */
  private final boolean verbose;

  /** Dry-run flag. */
  private final boolean dryRun;

  /** Initialize this instance with default values. */
  public Bach() {
    this(
        System.out::println,
        Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug")),
        Boolean.getBoolean("ry-run") || "".equals(System.getProperty("ry-run")));
  }

  /** Initialize this instance with the specified line printer and verbosity flag. */
  public Bach(Consumer<String> printer, boolean verbose, boolean dryRun) {
    this.printer = Objects.requireNonNull(printer, "printer");
    this.verbose = verbose;
    this.dryRun = dryRun;
    print(Level.DEBUG, "%s initialized", this);
    print(Level.TRACE, "\tverbose=%s", verbose);
    print(Level.TRACE, "\tdry-run=%s", dryRun);
  }

  /** Print a message at information level. */
  public String print(String format, Object... args) {
    return print(Level.INFO, format, args);
  }

  /** Print a message at specified level. */
  public String print(Level level, String format, Object... args) {
    var message = String.format(format, args);
    if (verbose || level.getSeverity() >= Level.INFO.getSeverity()) printer.accept(message);
    return message;
  }

  /** Build the given project using default settings. */
  public void build(Project project) {
    if (verbose) project.toStrings().forEach(this::print);
    if (project.structure().collections().isEmpty()) print("No module collection present");
    if (dryRun) return;
    print(Level.DEBUG, "TODO build(%s)", project.toNameAndVersion());
  }

  @Override
  public String toString() {
    return "Bach.java " + VERSION;
  }
}
