package de.sormuras.bach;

import static java.lang.System.Logger.Level.ALL;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.Logger.Level.WARNING;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.spi.ToolProvider;

/*BODY*/
/** Runtime context information. */
public /*STATIC*/ class Run {

  /** Declares property keys and their default values. */
  static class DefaultProperties extends Properties {
    DefaultProperties() {
      setProperty("debug", "false");
      setProperty("dry-run", "false");
      setProperty("threshold", INFO.name());
    }
  }

  /** Named property value getter. */
  private static class Configurator {

    final Properties properties;

    Configurator(Properties properties) {
      this.properties = properties;
    }

    String get(String key) {
      return System.getProperty(key, properties.getProperty(key));
    }

    boolean is(String key) {
      var value = System.getProperty(key.substring(1), properties.getProperty(key));
      return "".equals(value) || "true".equals(value);
    }

    private System.Logger.Level threshold() {
      if (is("debug")) {
        return ALL;
      }
      var level = get("threshold").toUpperCase();
      return System.Logger.Level.valueOf(level);
    }
  }

  /** Create default Run instance in user's current directory. */
  public static Run system() {
    return system(Path.of(""));
  }

  /** Create default Run instance in given home directory. */
  public static Run system(Path home) {
    var out = new PrintWriter(System.out, true, UTF_8);
    var err = new PrintWriter(System.err, true, UTF_8);
    return new Run(home, out, err, newProperties(home));
  }

  static Properties newProperties(Path home) {
    var properties = new Properties(new DefaultProperties());
    var names = new ArrayList<String>();
    if (home.getFileName() != null) {
      names.add(home.getFileName().toString());
    }
    names.addAll(List.of("bach", "build", ""));
    for (var name : names) {
      var path = home.resolve(name + ".properties");
      if (Files.exists(path)) {
        try (var stream = Files.newBufferedReader(path)) {
          properties.load(stream);
          break;
        } catch (Exception e) {
          throw new Error("Loading properties failed: " + path, e);
        }
      }
    }
    return properties;
  }

  /** Home directory. */
  final Path home;
  /** Current logging level threshold. */
  final System.Logger.Level threshold;
  /** Debug flag. */
  final boolean debug;
  /** Dry-run flag. */
  final boolean dryRun;
  /** Stream to which normal and expected output should be written. */
  final PrintWriter out;
  /** Stream to which any error messages should be written. */
  final PrintWriter err;
  /** Time instant recorded on creation of this instance. */
  final Instant start;

  Run(Path home, PrintWriter out, PrintWriter err, Properties properties) {
    this.start = Instant.now();
    this.home = home;
    this.out = out;
    this.err = err;

    var configurator = new Configurator(properties);
    this.debug = configurator.is("debug");
    this.dryRun = configurator.is("dry-run");
    this.threshold = configurator.threshold();
  }

  /** Log message unless threshold suppresses it. */
  void log(System.Logger.Level level, String format, Object... args) {
    if (level.getSeverity() < threshold.getSeverity()) {
      return;
    }
    var consumer = level.getSeverity() < WARNING.getSeverity() ? out : err;
    var message = String.format(format, args);
    consumer.println(message);
  }

  /** Run given command. */
  void run(Command command) {
    run(command.name, command.toStringArray());
  }

  /** Run named tool, as loaded by {@link java.util.ServiceLoader} using the system class loader. */
  void run(String name, String... args) {
    run(ToolProvider.findFirst(name).orElseThrow(), args);
  }

  /** Run provided tool. */
  void run(ToolProvider tool, String... args) {
    log(TRACE, "Run::run(%s, %s)", tool.name(), String.join(", ", args));
    var code = tool.run(out, err, args);
    if (code == 0) {
      log(DEBUG, "Tool '%s' successfully run.", tool.name());
      return;
    }
    throw new Error("Tool '" + tool.name() + "' run failed with error code: " + code);
  }

  long toDurationMillis() {
    return TimeUnit.MILLISECONDS.convert(Duration.between(start, Instant.now()));
  }

  @Override
  public String toString() {
    return String.format(
        "Run{debug=%s, dryRun=%s, threshold=%s, start=%s, out=%s, err=%s}",
        debug, dryRun, threshold, start, out, err);
  }
}
