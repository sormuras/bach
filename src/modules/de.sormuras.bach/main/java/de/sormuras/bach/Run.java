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
      setProperty("home", "");
      setProperty("work", "target/bach");
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

    private Path work(Path home) {
      var work = Path.of(get("work"));
      return work.isAbsolute() ? work : home.resolve(work);
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
    return new Run(newProperties(home), out, err);
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
  /** Workspace directory. */
  final Path work;
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

  Run(Properties properties, PrintWriter out, PrintWriter err) {
    this.start = Instant.now();
    this.out = out;
    this.err = err;

    var configurator = new Configurator(properties);
    this.home = Path.of(configurator.get("home"));
    this.work = configurator.work(home);
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

  void logState(System.Logger.Level level) {
    var homePath = home.toString();
    log(level, "home = %s", homePath.isEmpty() ? "<empty> (" + Bach.USER_PATH + ")" : homePath);
    log(level, "work = %s", work);
    log(level, "debug = %s", debug);
    log(level, "dry-run = %s", dryRun);
    log(level, "threshold = %s", threshold);
    log(level, "out = %s", out);
    log(level, "err = %s", err);
    log(level, "start = %s", start);
  }

  @Override
  public String toString() {
    return String.format("Run{home=%s, work=%s, debug=%s, dryRun=%s}", home, work, debug, dryRun);
  }
}
