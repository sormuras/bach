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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

/*BODY*/
/** Runtime context information. */
public /*STATIC*/ class Run {

  /** Declares property keys and their default values. */
  static class DefaultProperties extends Properties {
    DefaultProperties() {
      setProperty("home", "");
      setProperty("work", "");
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

    boolean is(String key, int trimLeft) {
      var value = System.getProperty(key.substring(trimLeft), properties.getProperty(key));
      return "".equals(value) || "true".equals(value);
    }

    private System.Logger.Level threshold() {
      if (is("debug", 1)) {
        return ALL;
      }
      var level = get("threshold").toUpperCase();
      return System.Logger.Level.valueOf(level);
    }

    private Path work(Path home) {
      var work = Path.of(get("work"));
      return work.isAbsolute() ? work : home.resolve(work);
    }

    private Map<String, String> variables() {
      var map = new HashMap<String, String>();
      for (var name : properties.stringPropertyNames()) {
        if (name.startsWith("${") && name.endsWith("}")) {
          map.put(name, properties.getProperty(name));
        }
      }
      return Map.copyOf(map);
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
    return loadProperties(properties, home);
  }

  static Properties loadProperties(Properties properties, Path home) {
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
  /** Offline flag. */
  private final boolean offline;
  /** Stream to which normal and expected output should be written. */
  final PrintWriter out;
  /** Stream to which any error messages should be written. */
  final PrintWriter err;
  /** Time instant recorded on creation of this instance. */
  final Instant start;
  /** User-defined variables. */
  final Map<String, String> variables;

  Run(Properties properties, PrintWriter out, PrintWriter err) {
    this.start = Instant.now();
    this.out = out;
    this.err = err;

    var configurator = new Configurator(properties);
    this.home = Path.of(configurator.get("home"));
    this.work = configurator.work(home);
    this.debug = configurator.is("debug", 1);
    this.dryRun = configurator.is("dry-run", 1);
    this.offline = configurator.is("offline", 0);
    this.threshold = configurator.threshold();
    this.variables = configurator.variables();
  }

  /** @return state of the offline flag */
  public boolean isOffline() {
    return offline;
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

  /** Replace each variable key declared in the given string by its value. */
  String replaceVariables(String string) {
    var result = string;
    for (var variable : variables.entrySet()) {
      result = result.replace(variable.getKey(), variable.getValue());
    }
    return result;
  }

  /** Run given command. */
  void run(Command command) {
    run(command.name, command.toStringArray());
  }

  /** Run named tool, as loaded by {@link java.util.ServiceLoader} using the system class loader. */
  void run(String name, String... args) {
    var providedTool = ToolProvider.findFirst(name);
    if (providedTool.isPresent()) {
      var tool = providedTool.get();
      log(DEBUG, "Running provided tool in-process: " + tool);
      run(tool, args);
      return;
    }
    log(DEBUG, "Starting new process for '%s'", name);
    var builder = newProcessBuilder(name);
    builder.command().addAll(List.of(args));
    run(builder);
  }

  /** Run provided tool. */
  void run(ToolProvider tool, String... args) {
    log(TRACE, "Run::run(%s, %s)", tool.name(), String.join(", ", args));
    var code = tool.run(out, err, args);
    if (code != 0) {
      throw new Error("Tool '" + tool.name() + "' run failed with error code: " + code);
    }
    log(DEBUG, "Tool '%s' successfully run.", tool.name());
  }

  /** Create new process builder for the given command and inherit IO from current process. */
  ProcessBuilder newProcessBuilder(String command) {
    var builder = new ProcessBuilder(command).inheritIO();
    builder.environment().put("BACH_VERSION", Bach.VERSION);
    builder.environment().put("BACH_HOME", home.toString());
    builder.environment().put("BACH_WORK", work.toString());
    return builder;
  }

  void run(ProcessBuilder builder) {
    log(TRACE, "Run::run(%s)", builder);
    try {
      var process = builder.start();
      var code = process.waitFor();
      if (code != 0) {
        throw new Error("Process '" + process + "' failed with error code: " + code);
      }
      log(DEBUG, "Process '%s' successfully terminated.", process);
    } catch (Exception e) {
      throw new Error("Starting process failed: " + e);
    }
  }

  long toDurationMillis() {
    return TimeUnit.MILLISECONDS.convert(Duration.between(start, Instant.now()));
  }

  @Override
  public String toString() {
    return String.format("Run{home=%s, work=%s, debug=%s, dryRun=%s}", home, work, debug, dryRun);
  }

  void toStrings(Consumer<String> consumer) {
    consumer.accept("home = '" + home + "' -> " + home.toUri());
    consumer.accept("work = '" + work + "' -> " + work.toUri());
    consumer.accept("debug = " + debug);
    consumer.accept("dry-run = " + dryRun);
    consumer.accept("offline = " + isOffline());
    consumer.accept("threshold = " + threshold);
    consumer.accept("out = " + out);
    consumer.accept("err = " + err);
    consumer.accept("start = " + start);
    consumer.accept("variables = " + variables);
  }
}
