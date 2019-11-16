/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

import java.io.PrintWriter;
import java.time.Instant;

/** Logbook. */
public class Log {

  /** Create new Log instance using system default text output streams. */
  public static Log ofSystem() {
    var verbose = Boolean.getBoolean("verbose");
    var debug = Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug"));
    return ofSystem(verbose || debug);
  }

  /** Create new Log instance using system default text output streams. */
  public static Log ofSystem(boolean verbose) {
    return new Log(new PrintWriter(System.out, true), new PrintWriter(System.err, true), verbose);
  }

  /** Instant of creation. */
  private final Instant created;
  /** Text-output writer. */
  private final PrintWriter out, err;
  /** Be verbose. */
  private final boolean verbose;

  protected Log(PrintWriter out, PrintWriter err, boolean verbose) {
    this.created = Instant.now();
    this.out = out;
    this.err = err;
    this.verbose = verbose;
  }

  /** Instant of creation. */
  public Instant created() {
    return created;
  }

  /** Print "debug" message to the standard output stream. */
  public void debug(String format, Object... args) {
    if (verbose) out.println(String.format(format, args));
  }

  /** Print "information" message to the standard output stream. */
  public void info(String format, Object... args) {
    out.println(String.format(format, args));
  }

  /** Print "warn" message to the error output stream. */
  public void warn(String format, Object... args) {
    err.println(String.format(format, args));
  }
}
