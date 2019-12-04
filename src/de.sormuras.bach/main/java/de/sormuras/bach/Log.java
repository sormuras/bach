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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/** Logbook. */
public class Log {

  /** Create new Log instance using {@link PrintWriter#nullWriter()} as target. */
  public static Log ofNullWriter() {
    var writer = PrintWriter.nullWriter();
    return new Log(new PrintWriter(writer), new PrintWriter(writer), false);
  }

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
  /** Task-related contexts. */
  private final Deque<Context> contexts;
  /** All log entries. */
  private final List<Entry> entries;
  /** All "simple text" messages. */
  private final List<String> messages;
  /** All tool runs. */
  private final List<Run> runs;
  /** Text-output writer. */
  /*package-private*/ final PrintWriter out, err;
  /** Be verbose. */
  /*package-private*/ final boolean verbose;

  protected Log(PrintWriter out, PrintWriter err, boolean verbose) {
    this.instant = Instant.now();
    this.contexts = new ArrayDeque<>();
    this.entries = new ArrayList<>();
    this.messages = new ArrayList<>();
    this.runs = new ArrayList<>();
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

  public List<Run> getRuns() {
    return runs;
  }

  Context context(Task task) {
    return new Context(task);
  }

  private Entry message(System.Logger.Level level, String format, Object... args) {
    var message = String.format(format, args);
    messages.add(message);
    var entry = new Entry(level, message);
    entries.add(entry);
    return entry;
  }

  /** Print "debug" message to the standard output stream. */
  public Entry debug(String format, Object... args) {
    var entry = message(System.Logger.Level.DEBUG, format, args);
    if (verbose) out.println(entry.message);
    return entry;
  }

  /** Print "information" message to the standard output stream. */
  public Entry info(String format, Object... args) {
    var entry = message(System.Logger.Level.INFO, format, args);
    out.println(entry.message);
    return entry;
  }

  /** Print "warning" message to the error output stream. */
  public Entry warning(String format, Object... args) {
    var entry = message(System.Logger.Level.WARNING, format, args);
    err.println(entry.message);
    return entry;
  }

  /** Log tool run. */
  void tool(String name, String[] args, Duration duration, int code) {
    var task =
        Optional.ofNullable(contexts.peek())
            .map(c -> c.task().getClass().getSimpleName())
            .orElse("<none>");
    runs.add(new Run(task, name, args, duration, code));
  }

  public class Context implements AutoCloseable {

    private final Task task;

    private Context(Task task) {
      this.task = task;
      contexts.push(this);
      // entry(Type.CONTEXT_BEGIN, System.Logger.Level.TRACE, task + " start.");
    }

    public Task task() {
      return task;
    }

    @Override
    public void close() {
      // entry(Type.CONTEXT_END, System.Logger.Level.TRACE, task + " end.");
      contexts.pop();
    }
  }

  public static /*record*/ class Entry {
    private final Instant instant;
    private final System.Logger.Level level;
    private final String message;

    private Entry(System.Logger.Level level, String message) {
      this.instant = Instant.now();
      this.level = level;
      this.message = message;
    }

    public Instant instant() {
      return instant;
    }

    public System.Logger.Level level() {
      return level;
    }

    public String message() {
      return message;
    }

    public boolean isWarning() {
      return level == System.Logger.Level.WARNING;
    }
  }

  public static /*record*/ class Run {
    private final String task;
    private final String name;
    private final String[] args;
    private final Duration duration;
    private final int code;

    Run(String task, String name, String[] args, Duration duration, int code) {
      this.task = task;
      this.name = name;
      this.args = args;
      this.duration = duration;
      this.code = code;
    }

    public String task() {
      return task;
    }

    public String name() {
      return name;
    }

    public String[] args() {
      return args;
    }

    public Duration duration() {
      return duration;
    }

    public int code() {
      return code;
    }
  }
}
