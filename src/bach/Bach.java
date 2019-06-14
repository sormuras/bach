// THIS FILE WAS GENERATED ON 2019-06-14T09:10:21.067284500Z
/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// default package

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** Java Shell Builder. */
public class Bach {

  /** Version is either {@code master} or {@link Runtime.Version#parse(String)}-compatible. */
  public static final String VERSION = "master";

  /** Convenient short-cut to {@code "user.home"} as a path. */
  static final Path USER_HOME = Path.of(System.getProperty("user.home"));

  /** Convenient short-cut to {@code "user.dir"} as a path. */
  static final Path USER_PATH = Path.of(System.getProperty("user.dir"));

  /** Main entry-point making use of {@link System#exit(int)} on error. */
  public static void main(String... arguments) {
    var bach = new Bach();
    var args = List.of(arguments);
    var code = bach.main(args);
    if (code != 0) {
      System.err.printf("Bach main(%s) failed with error code: %d%n", args, code);
      System.exit(code);
    }
  }

  final Project project = new Project("bach", VERSION);
  final Run run;

  public Bach() {
    this(Run.system());
  }

  public Bach(Run run) {
    this.run = run;
    run.log("%s initialized", this);
  }

  /** Main entry-point, by convention, a zero status code indicates normal termination. */
  int main(List<String> arguments) {
    run.info("main(%s)", arguments);
    if (List.of("42").equals(arguments)) {
      return 42;
    }
    if (run.dryRun) {
      run.info("Dry-run ends here.");
      return 0;
    }
    return 0;
  }

  @Override
  public String toString() {
    return "Bach (" + VERSION + ")";
  }

  /** Project data. */
  public static class Project {
    final String name;
    final String version;

    Project(String name, String version) {
      this.name = name;
      this.version = version;
    }

    @Override
    public String toString() {
      return name + ' ' + version;
    }
  }

  /** Command-line program argument list builder. */
  public static class Command {

    final String name;
    final List<String> list = new ArrayList<>();

    /** Initialize Command instance with zero or more arguments. */
    Command(String name, Object... args) {
      this.name = name;
      addEach(args);
    }

    /** Add single argument by invoking {@link Object#toString()} on the given argument. */
    Command add(Object argument) {
      list.add(argument.toString());
      return this;
    }

    /** Add two arguments by invoking {@link #add(Object)} for the key and value elements. */
    Command add(Object key, Object value) {
      return add(key).add(value);
    }

    /** Add two arguments, i.e. the key and the paths joined by system's path separator. */
    Command add(Object key, Collection<Path> paths) {
      return add(key, paths, UnaryOperator.identity());
    }

    /** Add two arguments, i.e. the key and the paths joined by system's path separator. */
    Command add(Object key, Collection<Path> paths, UnaryOperator<String> operator) {
      var stream = paths.stream() /*.filter(Files::isDirectory)*/.map(Object::toString);
      return add(key, operator.apply(stream.collect(Collectors.joining(File.pathSeparator))));
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    Command addEach(Object... arguments) {
      return addEach(List.of(arguments));
    }

    /** Add all arguments by invoking {@link #add(Object)} for each element. */
    Command addEach(Iterable<?> arguments) {
      arguments.forEach(this::add);
      return this;
    }

    /** Add a single argument iff the conditions is {@code true}. */
    Command addIff(boolean condition, Object argument) {
      return condition ? add(argument) : this;
    }

    /** Let the consumer visit, usually modify, this instance iff the conditions is {@code true}. */
    Command addIff(boolean condition, Consumer<Command> visitor) {
      if (condition) {
        visitor.accept(this);
      }
      return this;
    }

    @Override
    public String toString() {
      var args = list.isEmpty() ? "<empty>" : "'" + String.join("', '", list) + "'";
      return "Command{name='" + name + "', list=[" + args + "]}";
    }

    /** Returns an array of {@link String} containing all of the collected arguments. */
    String[] toStringArray() {
      return list.toArray(String[]::new);
    }
  }

  /** Runtime context information. */
  public static class Run {

    /** Named property value getter. */
    private static class Configurator {
      final Properties properties;

      Configurator(Properties properties) {
        this.properties = properties;
      }

      String get(String key, String defaultValue) {
        return System.getProperty(key, properties.getProperty(key, defaultValue));
      }

      boolean is(String key, String defaultValue) {
        var value = System.getProperty(key.substring(1), properties.getProperty(key, defaultValue));
        return "".equals(value) || "true".equals(value);
      }

      private System.Logger.Level threshold() {
        if (is("debug", "false")) {
          return DEBUG;
        }
        var level = get("threshold", "INFO").toUpperCase();
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

    /** Create default properties. */
    static Properties defaultProperties() {
      var defaults = new Properties();
      defaults.setProperty("debug", "false");
      defaults.setProperty("dry-run", "false");
      defaults.setProperty("threshold", INFO.name());
      return defaults;
    }

    static Properties newProperties(Path home) {
      var properties = new Properties(defaultProperties());
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
      this.debug = configurator.is("debug", "false");
      this.dryRun = configurator.is("dry-run", "false");
      this.threshold = configurator.threshold();
    }

    /** Log debug message unless threshold suppresses it. */
    void info(String format, Object... args) {
      log(INFO, format, args);
    }

    /** Log debug message unless threshold suppresses it. */
    void log(String format, Object... args) {
      log(DEBUG, format, args);
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
      log("Running tool '%s' with: %s", tool.name(), List.of(args));
      var code = tool.run(out, err, args);
      if (code == 0) {
        log("Tool '%s' successfully run.", tool.name());
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
}
