package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.project.Project;
import com.github.sormuras.bach.tool.ToolResponse;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** A logbook records text messages. */
public final class Logbook implements Consumer<String> {

  record Entry(long thread, Level level, String text) {

    @Override
    public String toString() {
      return String.format("%-7s %6X| %s", level, thread, text);
    }
  }

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

  static String markdown(Object object) {
    return object.toString().replace('\t', ' ').replaceAll("\\e\\[[\\d;]*[^\\d;]","");
  }

  static String markdownJoin(Collection<?> collection) {
    if (collection.isEmpty()) return "`-`";
    return collection.stream()
        .map(Object::toString)
        .sorted()
        .collect(Collectors.joining("`, `", "`", "`"));
  }

  static String markdownAnchor(ToolResponse response) {
    return response.name() + '-' + Integer.toHexString(System.identityHashCode(response));
  }

  private final LocalDateTime created = LocalDateTime.now(ZoneOffset.UTC);
  private final Queue<Entry> entries = new ConcurrentLinkedQueue<>();
  private final Queue<ToolResponse> responses = new ConcurrentLinkedQueue<>();
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

  /**
   * Adds the given response to the list of responses.
   * @param response the tool call response
   */
  public void log(ToolResponse response) {
    synchronized (responses) {
      responses.add(response);
    }
  }

  List<ToolResponse> responses(Predicate<ToolResponse> predicate) {
    return responses.stream().filter(predicate).collect(Collectors.toList());
  }

  List<String> toMarkdown(Project project) {
    var md = new ArrayList<String>();
    var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    md.add("# Logbook of " + project.name() + ' ' + project.version());
    md.add("");
    md.add("- Created at " + formatter.format(created));
    md.add("- Written at " + formatter.format(LocalDateTime.now(ZoneOffset.UTC)));
    md.addAll(toModulesOverview(Bach.WORKSPACE.resolve("modules")));
    // md.addAll(projectDescription(project));
    md.addAll(toToolsOverview());
    md.addAll(toToolsDetails());
    md.addAll(toLogbookEntries());
    md.add("");
    md.add("## Thanks for using Bach.java " + Bach.version());
    md.add("");
    md.add("Support its development at <https://github.com/sponsors/sormuras>");
    return md;
  }

  private List<String> toModulesOverview(Path directory) {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Modules");
    md.add("");
    if (!Files.isDirectory(directory)) {
      md.add(String.format("Directory `%s` doesn't exist or isn't a directory.", directory));
      return md;
    }
    var files = Paths.list(directory, Paths::isJarFile);
    md.add("- directory: " + directory.toUri());
    md.add("- files: " + files.size());
    if (files.isEmpty()) return md;
    md.add("");
    md.add("### Module API");
    md.add("");
    md.add("| Name | Version | Exports | Provides | Main Class |");
    md.add("|------|---------|---------|----------|------------|");
    for (var file : files) {
      var descriptor = ModuleFinder.of(file).findAll().iterator().next().descriptor();
      var module = descriptor.name();
      var version = descriptor.version().map(Object::toString).orElse("-");
      var exports = markdownJoin(descriptor.exports());
      var provides = markdownJoin(descriptor.provides());
      var main = descriptor.mainClass().map(Object::toString).orElse("-");
      md.add(String.format("|`%s`|%s|%s|%s|`%s`|", module, version, exports, provides, main));
    }
    md.add("");
    md.add("### Modular JAR");
    md.add("");
    md.add("| Size [Bytes] | File Name |");
    md.add("|-------------:|:----------|");
    for (var file : files) {
      var size = Paths.size(file);
      var name = file.getFileName();
      md.add(String.format("|%,d|%s", size, name));
    }
    return md;
  }

  private List<String> toToolsOverview() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Tool Call Overview");
    md.add("");
    md.add("|    |Thread| Duration |Tool|Arguments");
    md.add("|----|-----:|---------:|----|---------");
    for (var response : responses) {
      var kind = ' ';
      var thread = response.thread();
      var millis = toString(response.duration());
      var tool = "[" + response.name() + "](#" + markdownAnchor(response) + ")";
      var arguments = "`" + String.join(" ", response.args()) + "`";
      var row = String.format("|%4c|%6X|%10s|%s|%s", kind, thread, millis, tool, arguments);
      md.add(row);
    }
    return md;
  }

  private List<String> toToolsDetails() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Tool Call Details");
    md.add("");
    md.add(String.format("Recorded %d tool call response(s).", responses.size()));
    for (var response : responses) {
      md.add("");
      md.add("### " + markdownAnchor(response));
      md.add("");
      md.add("- tool = `" + response.name() + '`');
      md.add("- args = `" + String.join(" ", response.args()) + '`');
      if (!response.out().isEmpty()) {
        md.add("");
        md.add("```text");
        md.add(markdown(response.out()));
        md.add("```");
      }
      if (!response.err().isEmpty()) {
        md.add("");
        md.add("```text");
        md.add(markdown(response.err()));
        md.add("```");
      }
    }
    return md;
  }

  private List<String> toLogbookEntries() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## All Entries");
    md.add("");
    md.add("```text");
    for (var entry : entries) md.add(markdown(entry));
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
    var workspace = Bach.WORKSPACE;
    var markdownFile = workspace.resolve("logbook.md");
    var markdownLines = toMarkdown(project);
    try {
      Paths.createDirectories(workspace);
      Files.write(markdownFile, markdownLines);

      var formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
      var timestamp = formatter.format(created);
      var logbooks = Paths.createDirectories(workspace.resolve("logbooks"));
      Files.write(logbooks.resolve("logbook-" + timestamp + ".md"), markdownLines);
    } catch (Exception exception) {
      var message = log(Level.ERROR, "Writing logbook failed: %s", exception);
      throw new AssertionError(message, exception);
    }
    return markdownFile;
  }
}
