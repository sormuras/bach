package run.bach;

import java.io.PrintWriter;
import java.io.Writer;
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

  public static final Printer BROKEN = new Printer(new PrintWriter(Writer.nullWriter()));

  public Printer(PrintWriter writer) {
    this(writer, writer, System.Logger.Level.INFO, 1000);
  }

  public Printer(PrintWriter out, PrintWriter err, System.Logger.Level threshold, int margin) {
    this(out, err, threshold, margin, new ConcurrentLinkedDeque<>());
  }

  public void out(Object object) {
    var text = object.toString();
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

  public void log(System.Logger.Level level, Object object) {
    var text = object.toString();
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
    out(text);
  }

  public record Message(Instant instant, System.Logger.Level level, String text) {}
}
