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
import java.util.ArrayList;
import java.util.List;

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
  private final Instant instant;
  /** All log entries. */
  private final List<Entry> entries;
  /** All "simple text" messages. */
  private final List<String> messages;
  /** Text-output writer. */
  private final PrintWriter out, err;
  /** Be verbose. */
  private final boolean verbose;

  protected Log(PrintWriter out, PrintWriter err, boolean verbose) {
    this.instant = Instant.now();
    this.entries = new ArrayList<>();
    this.messages = new ArrayList<>();
    this.out = out;
    this.err = err;
    this.verbose = verbose;
  }

  /** Instant of creation. */
  public Instant getInstant() {
    return instant;
  }

  public List<Entry> getEntries() {
    return entries;
  }

  public List<String> getMessages() {
    return messages;
  }

  private String message(System.Logger.Level level, String format, Object... args) {
    var message = String.format(format, args);
    messages.add(message);
    entries.add(new Entry(level, message));
    return message;
  }

  /** Print "debug" message to the standard output stream. */
  public void debug(String format, Object... args) {
    var message = message(System.Logger.Level.DEBUG, format, args);
    if (verbose) out.println(message);
  }

  /** Print "information" message to the standard output stream. */
  public void info(String format, Object... args) {
    out.println(message(System.Logger.Level.INFO, format, args));
  }

  /** Print "warning" message to the error output stream. */
  public void warning(String format, Object... args) {
    err.println(message(System.Logger.Level.WARNING, format, args));
  }

  public static /*record*/ class Entry {
    public final Instant instant;
    public final System.Logger.Level level;
    public final String message;

    private Entry(System.Logger.Level level, String message) {
      this.instant = Instant.now();
      this.level = level;
      this.message = message;
    }

    public boolean isWarning() {
      return level == System.Logger.Level.WARNING;
    }
  }
}
