package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Settings.LogbookSettings;
import java.io.PrintWriter;
import java.lang.System.Logger.Level;
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

  public static Logbook of(LogbookSettings settings) {
    return Logbook.of(settings.out(), settings.err(), settings.verbose());
  }

  public static Logbook of(PrintWriter out, PrintWriter err, boolean verbose) {
    return new Logbook(
        out,
        err,
        verbose,
        new ConcurrentLinkedQueue<>(),
        new ConcurrentLinkedQueue<>(),
        new ConcurrentLinkedQueue<>());
  }

  public void log(Level level, String text) {
    messages.add(new Message(level, text));
    if (level.getSeverity() >= Level.ERROR.getSeverity()) {
      err.println(text);
      return;
    }
    if (level.getSeverity() >= Level.WARNING.getSeverity()) {
      out.println(text);
      return;
    }
    if (verbose || level.getSeverity() >= Level.INFO.getSeverity()) {
      out.println(text);
    }
  }

  public void log(Exception exception) {
    log(Level.ERROR, "Exception: %s".formatted(exception));
    exceptions.add(exception);
  }

  public void log(Run run) {
    log(run.isSuccessful() ? Level.DEBUG : Level.WARNING, "Run: %s".formatted(run));
    runs.add(run);
  }

  public Stream<String> lines() {
    return lines(__ -> true);
  }

  public Stream<String> lines(Predicate<Message> filter) {
    return messages.stream().filter(filter).map(Message::text);
  }

  public record Message(Level level, String text) {}
}
