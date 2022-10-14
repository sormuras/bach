package run.bach;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.Deque;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedDeque;

public record Printer(
    PrintWriter out,
    PrintWriter err,
    System.Logger.Level threshold,
    int margin,
    Deque<Message> history) {

  public record Message(Instant instant, System.Logger.Level level, String text) {}

  public Printer(PrintWriter out, PrintWriter err, System.Logger.Level threshold, int margin) {
    this(out, err, threshold, margin, new ConcurrentLinkedDeque<>());
  }

  public void printDirect(String text) {
    if (text.length() <= margin) {
      out.println(text);
      return;
    }
    var lines = new StringJoiner("\n");
    for (var line : text.lines().toList()) {
      lines.add(line.length() <= margin ? line : line.substring(0, margin - 3) + "...");
    }
    out.println(lines);
  }

  public void printMessage(System.Logger.Level level, String text) {
    history.add(new Message(Instant.now(), level, text));
    if (threshold == System.Logger.Level.OFF) {
      return;
    }
    var severity = level.getSeverity();
    if (severity >= System.Logger.Level.ERROR.getSeverity()) {
      err.println(text); // ignore margin
      return;
    }
    if (threshold != System.Logger.Level.ALL && severity < threshold.getSeverity()) {
      return;
    }
    printDirect(text);
  }

  public String toString(int indent) {
    var joiner = new StringJoiner("\n");
    joiner.add("out       = " + out);
    joiner.add("err       = " + err);
    joiner.add("threshold = " + threshold);
    joiner.add("margin    = " + margin);
    return joiner.toString().indent(indent).stripTrailing();
  }

  public String toHistoryString(int indent) {
    var joiner = new StringJoiner("\n");
    for (var message : history) {
      joiner.add("| %s %s".formatted(message.level, message.instant));
      joiner.add(message.text);
    }
    return joiner.toString().indent(indent).stripTrailing();
  }
}
