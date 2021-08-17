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

public class Logbook {

  public record CaptionNote(String line) implements Note {}

  public record MessageNote(Level level, String text) implements Note {}

  public record RunNote(Run run, String description) implements Note {}

  private final Queue<Note> notes;
  private final LocalDateTime start;
  private final String timestamp;

  public Logbook() {
    this.notes = new ConcurrentLinkedQueue<>();
    this.start = LocalDateTime.now(ZoneOffset.UTC);
    this.timestamp = DateTimeFormatter.ofPattern(Configuration.TIMESTAMP_PATTERN).format(start);
  }

  public Duration uptime() {
    return Duration.between(start, LocalDateTime.now(ZoneOffset.UTC));
  }

  public void add(Note note) {
    notes.add(note);
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
      if (note instanceof RunNote ran) {
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
