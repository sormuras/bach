package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.DurationSupport;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** A logbook collects notes, prints them, and is able to write itself into a file. */
public class Logbook {

  /** An abstract logbook entry. */
  public sealed interface Note permits CaptionNote, MessageNote, ToolCallNote, ToolRunNote {}

  /** A textual note starting a section of notes. */
  public record CaptionNote(String line) implements Note {}

  /** A weighted textual note. */
  public record MessageNote(Level level, String text) implements Note {}

  /** A note indicating that a tool is about to be called.  */
  public record ToolCallNote(ToolCall call) implements Note {}

  /** A note indicating that a tool was run with a detailed description.  */
  public record ToolRunNote(ToolRun run, String description) implements Note {}

  private final Bach bach;
  private final Queue<Note> notes;
  private final LocalDateTime start;
  private final String timestamp;

  public Logbook(Bach bach) {
    this.bach = bach;
    this.notes = new ConcurrentLinkedQueue<>();
    this.start = LocalDateTime.now(ZoneOffset.UTC);
    this.timestamp = DateTimeFormatter.ofPattern(Configuration.TIMESTAMP_PATTERN).format(start);
  }

  public Duration uptime() {
    return Duration.between(start, LocalDateTime.now(ZoneOffset.UTC));
  }

  private <N extends Note> N add(N note) {
    notes.add(note);
    return note;
  }

  public void logCaption(String line) {
    print(add(new CaptionNote(line)));
  }

  public void logMessage(Level level, String text) {
    print(add(new MessageNote(level, text)));
  }

  void logToolCall(ToolCall call) {
    print(add(new ToolCallNote(call)));
  }

  void logToolRun(ToolRun run, String description) {
    print(add(new ToolRunNote(run, description)));
  }

  protected void print(CaptionNote note) {
    bach.out().println();
    bach.out().println(note.line());
  }

  protected void print(MessageNote note) {
    var severity = note.level().getSeverity();
    var text = note.text();
    if (severity >= Level.ERROR.getSeverity()) {
      bach.err().println(text);
      return;
    }
    if (severity >= Level.WARNING.getSeverity()) {
      bach.out().println(text);
      return;
    }
    if (severity >= Level.INFO.getSeverity() || bach.configuration().verbose()) {
      bach.out().println(text);
    }
  }

  protected void print(ToolCallNote note) {
    var builder = new StringBuilder("%16s".formatted(note.call().name()));
    for (var argument : note.call().arguments()) {
      builder.append(" ").append(String.join("\\n", argument.lines().toList()));
    }
    var string = builder.toString();
    var line = string.length() <= 111 ? string : string.substring(0, 111 - 3) + "...";
    bach.out().println(line);
  }

  protected void print(ToolRunNote note) {
    var run = note.run();
    var printer = run.isError() ? bach.err() : bach.out();
    if (run.isError() || bach.configuration().verbose()) {
      var output = run.output();
      var errors = run.errors();
      if (!output.isEmpty()) printer.println(output.indent(4).stripTrailing());
      if (!errors.isEmpty()) printer.println(errors.indent(4).stripTrailing());
      printer.printf(
          "Tool '%s' run with %d argument%s took %s and finished with exit code %d%n",
          run.name(),
          run.args().size(),
          run.args().size() == 1 ? "" : "s",
          DurationSupport.toHumanReadableString(run.duration()),
          run.code());
    }
  }

  public String toMarkdown() {
    var md = new ArrayList<String>();
    md.add("# Bach Logbook");
    md.add("");
    md.add("- start: " + start);
    md.add("");
    md.add("## Notes");
    md.add("");
    for (var note : notes) {
      if (note instanceof CaptionNote caption) {
        md.add("");
        md.add("**" + sanitize(caption.line()) + "**");
        md.add("");
        continue;
      }
      if (note instanceof MessageNote message) {
        md.add("- [%s] %s".formatted(message.level().name(), sanitize(message.text())));
        continue;
      }
      if (note instanceof ToolRunNote ran) {
        var run = ran.run();
        var thread = run.thread() == 1 ? "main" : "0x%x".formatted(run.thread());
        var duration = DurationSupport.toHumanReadableString(run.duration());
        md.add(
            """

            <details>
            <summary>%s (%s):<code>%s</code></summary>
            """
                .formatted(duration, thread, run.name().strip()));
        var command = run.name() + ' ' + String.join(" ", run.args());
        md.add("> - `%s`".formatted(command.stripTrailing()));
        md.add("> - executed by = " + ran.description());
        md.add("> - executed in thread = " + run.thread());
        md.add("> - exit code = " + run.code());
        md.add("> - duration = " + duration);
        if (!run.output().isEmpty()) {
          md.add("> ");
          md.add("> ```text");
          run.output().lines().map(line -> "> " + sanitize(line)).forEach(md::add);
          md.add("> ```");
        }
        if (!run.errors().isEmpty()) {
          md.add("");
          md.add("```text");
          run.errors().lines().map(line -> "> " + sanitize(line)).forEach(md::add);
          md.add("```");
        }
        md.add("""

          </details>
          """);
        continue;
      }
      md.add("- `" + sanitize(note) + "`");
    }
    md.add(
        """

        ## Thanks for using Bach

        Support its development at <https://github.com/sponsors/sormuras>
        """);
    return String.join("\n", md);
  }

  Path write(Path directory) throws Exception {
    var logbookFile = directory.resolve(Configuration.LOGBOOK_MARKDOWN_FILE);
    var archiveName = Configuration.LOGBOOK_ARCHIVE_FILE.replace("{TIMESTAMP}", timestamp);
    var archiveFile = directory.resolve(archiveName);
    Files.createDirectories(logbookFile.getParent());
    Files.writeString(logbookFile, toMarkdown());
    Files.createDirectories(archiveFile.getParent());
    Files.copy(logbookFile, archiveFile, StandardCopyOption.COPY_ATTRIBUTES);
    return logbookFile;
  }

  public static String sanitize(Object object) {
    return object.toString().replace('\t', ' ').replaceAll("\\e\\[[\\d;]*[^\\d;]", "");
  }
}
