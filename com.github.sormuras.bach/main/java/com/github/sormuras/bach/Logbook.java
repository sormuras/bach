package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Paths;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** A logbook records text messages. */
public final class Logbook implements Consumer<String> {

  record Entry(long thread, Level level, String text) {}

  /**
   * Returns a logbook initialized with default components.
   *
   * @return a logbook initialized with default components
   */
  public static Logbook ofSystem() {
    return new Logbook(System.out::println, defaultThresholdLevel());
  }

  static Level defaultThresholdLevel() {
    var debug = Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug"));
    var logbookThreshold = System.getProperty("bach.logbook.threshold", debug ? "ALL" : "INFO");
    return Level.valueOf(logbookThreshold);
  }

  static String toString(Duration duration) {
    return duration
        .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
        .toString()
        .substring(2)
        .replaceAll("(\\d[HMS])(?!$)", "$1 ")
        .toLowerCase();
  }

  private final LocalDateTime created = LocalDateTime.now(ZoneOffset.UTC);
  private final Queue<Entry> entries = new ConcurrentLinkedQueue<>();
  private final Consumer<String> printer;
  private final Level threshold;

  /**
   * Initialize a logbook with the given components.
   *
   * @param printer the {@code String}-consuming printer
   * @param threshold the level to also send log messages directly to the printer
   */
  public Logbook(Consumer<String> printer, Level threshold) {
    this.printer = printer;
    this.threshold = threshold;
  }

  @Override
  public void accept(String text) {
    printer.accept(text);
  }

  private boolean isOn(Level level) {
    return level.getSeverity() >= threshold.getSeverity();
  }

  private boolean isOff(Level level) {
    return level.getSeverity() < threshold.getSeverity();
  }

  /**
   * Returns the formatted message after logging it.
   *
   * @param level the level
   * @param format the format
   * @param arguments the arguments
   * @return the formatted message
   */
  public String log(Level level, String format, Object... arguments) {
    return log(level, arguments.length == 0 ? format : String.format(format, arguments));
  }

  /**
   * Returns the formatted message after logging it.
   *
   * @param level the level
   * @param text the message
   * @return the message
   */
  public String log(Level level, String text) {
    return log(level, text, true);
  }

  private String log(Level level, String text, boolean add) {
    if (text.isEmpty()) return text;
    if (text.equals("\n")) {
      if (isOn(level)) accept("");
      return text;
    }
    var thread = Thread.currentThread().getId();
    var entry = new Entry(thread, level, text);
    if (add) entries.add(entry);
    if (isOff(level)) return text;
    synchronized (entries) {
      var all = threshold == Level.ALL;
      var warning = level.getSeverity() >= Level.WARNING.getSeverity();
      accept(all ? entry.toString() : warning ? level.getName() + ' ' + text : text);
    }
    return text;
  }

  List<String> toMarkdown(Project project) {
    var md = new ArrayList<String>();
    var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    md.add("# Logbook of " + project.name() + ' ' + project.version());
    md.add("");
    md.add("- Created at " + formatter.format(created));
    md.add("- Written at " + formatter.format(LocalDateTime.now(ZoneOffset.UTC)));
    // md.addAll(projectModules(project.base().modules("")));
    // md.addAll(projectDescription(project));
    // md.addAll(toToolCallOverview());
    // md.addAll(toToolCallDetails());
    md.addAll(toLogbookEntries());
    md.add("");
    md.add("## Thanks for using Bach.java " + Bach.version());
    md.add("");
    md.add("Support its development at <https://github.com/sponsors/sormuras>");
    return md;
  }

  private List<String> toLogbookEntries() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## All Entries");
    md.add("");
    md.add("```text");
    for (var entry : entries) {
      var line = String.format("%-7s %6X| %s", entry.level, entry.thread, entry.text)
          .replace('\t', ' ');
      md.add(line);
    }
    md.add("```");
    return md;
  }

  /**
   * Returns the path to the logbook file.
   *
   * @param project the project that issued the messages
   * @return the path to the logbook file
   */
  public Path write(Project project) {
    var base = Path.of("");
    var markdownFile = Project.WORKSPACE.resolve("logbook.md");
    var markdownLines = toMarkdown(project);
    try {
      Paths.createDirectories(Project.WORKSPACE);
      Files.write(markdownFile, markdownLines);

      var formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
      var timestamp = formatter.format(created);
      var logbooks = Paths.createDirectories(Project.WORKSPACE.resolve("logbooks"));
      Files.write(logbooks.resolve("logbook-" + timestamp + ".md"), markdownLines);
    } catch (Exception exception) {
      var message = log(Level.ERROR, "Writing logbook failed: %s", exception);
      throw new AssertionError(message, exception);
    }
    return markdownFile;
  }
}
