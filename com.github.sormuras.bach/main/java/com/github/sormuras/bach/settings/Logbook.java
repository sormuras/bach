package com.github.sormuras.bach.settings;

import java.io.PrintWriter;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record Logbook(
    PrintWriter out,
    PrintWriter err,
    boolean verbose,
    Queue<Message> messages,
    Queue<Exception> exceptions,
    Queue<Run> runs) {

  public static Logbook of(PrintWriter out, PrintWriter err, boolean verbose) {
    return new Logbook(
        out,
        err,
        verbose,
        new ConcurrentLinkedQueue<>(),
        new ConcurrentLinkedQueue<>(),
        new ConcurrentLinkedQueue<>());
  }

  public static Logbook ofSystem() {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    return Logbook.of(out, err, false);
  }

  public void debug(String message) {
    log(System.Logger.Level.DEBUG, message);
  }

  public void info(String message) {
    log(System.Logger.Level.INFO, message);
  }

  public void log(System.Logger.Level level, String text) {
    messages.add(new Message(level, text));
    if (level.getSeverity() >= System.Logger.Level.ERROR.getSeverity()) {
      err.println(text);
      return;
    }
    if (level.getSeverity() >= System.Logger.Level.WARNING.getSeverity()) {
      out.println(text);
      return;
    }
    if (verbose || level.getSeverity() >= System.Logger.Level.INFO.getSeverity()) {
      out.println(text);
    }
  }

  public void log(Exception exception) {
    log(System.Logger.Level.ERROR, "Exception: %s".formatted(exception));
    exceptions.add(exception);
  }

  public void log(Run run) {
    log(run.isSuccessful() ? System.Logger.Level.DEBUG : System.Logger.Level.WARNING, "Run: %s".formatted(run));
    runs.add(run);
  }

  public Stream<String> lines() {
    return lines(__ -> true);
  }

  public Stream<String> lines(Predicate<Message> filter) {
    return messages.stream().filter(filter).map(Message::text);
  }

  public record Message(System.Logger.Level level, String text) {}

  /**
   * A recording of a tool call run.
   *
   * @param name     the name of the tool
   * @param args     the arguments of the tool run
   * @param thread   the ID of the thread that ran the tool
   * @param duration the duration of the tool run
   * @param code     the exit code of the tool run
   * @param output   the normal and expected output of the tool run
   * @param errors   the error message of the tool run
   */
  public record Run(
      String name,
      List<String> args,
      long thread,
      Duration duration,
      int code,
      String output,
      String errors) {

    /**
     * {@return {@code true} if this response represents an errored tool call run}
     */
    public boolean isError() {
      return code != 0;
    }

    /**
     * {@return {@code true} if this response represents a successful tool call run}
     */
    public boolean isSuccessful() {
      return code == 0;
    }

    /**
     * Returns silently if this response represents a successful tool call run.
     *
     * @throws RuntimeException if {@link #isError()} returns {@code true}
     */
    public void requireSuccessful() {
      if (isSuccessful()) return;
      var message = "%s returned code %d\n%s".formatted(name, code, toString().indent(4));
      throw new RuntimeException(message);
    }
  }
}
