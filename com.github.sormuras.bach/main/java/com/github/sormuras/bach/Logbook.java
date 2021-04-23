package com.github.sormuras.bach;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record Logbook(Printer printer, boolean verbose, Queue<Message> messages) {

  public static Logbook of() {
    return Logbook.of(Printer.of(), false);
  }

  public static Logbook of(Printer printer, boolean verbose) {
    return new Logbook(printer, verbose, new ConcurrentLinkedQueue<>());
  }

  public static Logbook ofErrorPrinter() {
    var out = new PrintWriter(Writer.nullWriter());
    var err = new PrintWriter(System.err, true);
    return Logbook.of(Printer.of(out, err), true);
  }

  public void log(System.Logger.Level level, String text) {
    messages.add(new Message(level, text));
    if (level.getSeverity() >= System.Logger.Level.ERROR.getSeverity()) {
      printer.err().println(text);
      return;
    }
    if (level.getSeverity() >= System.Logger.Level.WARNING.getSeverity()) {
      printer.out().println(text);
      return;
    }
    if (verbose || level.getSeverity() >= System.Logger.Level.INFO.getSeverity()) {
      printer.out().println(text);
    }
  }

  public void log(Exception exception) {
    log(System.Logger.Level.ERROR, exception.toString());
  }

  public Stream<String> lines() {
    return lines(__ -> true);
  }

  public Stream<String> lines(Predicate<Message> filter) {
    return messages.stream().filter(filter).map(Logbook.Message::text);
  }

  public record Message(System.Logger.Level level, String text) {}
}
