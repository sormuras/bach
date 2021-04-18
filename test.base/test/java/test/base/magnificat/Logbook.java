package test.base.magnificat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public record Logbook(Printer printer, boolean verbose, Queue<Message> messages) {

  public static Logbook of(Printer printers, boolean verbose) {
    return new Logbook(printers, verbose, new ConcurrentLinkedQueue<>());
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

  public record Message(System.Logger.Level level, String text) {}
}
