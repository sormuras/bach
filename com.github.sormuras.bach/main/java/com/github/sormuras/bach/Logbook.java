package com.github.sormuras.bach;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record Logbook(
    Printer printer,
    boolean verbose,
    Queue<Message> messages,
    Queue<Exception> exceptions,
    Queue<ToolRun> runs) {

  public static Logbook of() {
    return Logbook.of(Printer.of(), false);
  }

  public static Logbook of(Printer printer, boolean verbose) {
    return new Logbook(
        printer,
        verbose,
        new ConcurrentLinkedQueue<>(),
        new ConcurrentLinkedQueue<>(),
        new ConcurrentLinkedQueue<>());
  }

  public static Logbook ofErrorPrinter() {
    var out = new PrintWriter(Writer.nullWriter());
    var err = new PrintWriter(System.err, true);
    return Logbook.of(Printer.of(out, err), true);
  }

  Logbook verbose(boolean verbose) {
    return new Logbook(printer, verbose, messages, exceptions, runs);
  }

  public void debug(String message) {
    log(System.Logger.Level.DEBUG, message);
  }

  public void info(String message) {
    log(System.Logger.Level.INFO, message);
  }

  public void log(Level level, String text) {
    messages.add(new Message(level, text));
    if (level.getSeverity() >= Level.ERROR.getSeverity()) {
      printer.err().println(text);
      return;
    }
    if (level.getSeverity() >= Level.WARNING.getSeverity()) {
      printer.out().println(text);
      return;
    }
    if (verbose || level.getSeverity() >= Level.INFO.getSeverity()) {
      printer.out().println(text);
    }
  }

  public void log(Exception exception) {
    log(Level.ERROR, "Exception: " + exception.toString());
    exceptions.add(exception);
  }

  public void log(ToolRun run) {
    if (run.isError()) log(Level.ERROR, "Non-zero tool call run: " + run);
    runs.add(run);
  }

  public Stream<String> lines() {
    return lines(__ -> true);
  }

  public Stream<String> lines(Predicate<Message> filter) {
    return messages.stream().filter(filter).map(Logbook.Message::text);
  }

  public record Message(Level level, String text) {}
}
