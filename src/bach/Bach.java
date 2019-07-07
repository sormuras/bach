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

import static java.lang.System.Logger.Level.ALL;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** Java Shell Builder. */
public class Bach {

  /** Version of Bach, {@link Runtime.Version#parse(String)}-compatible. */
  public static final String VERSION = "2-ea";

  /**
   * Create new Bach instance with default properties.
   *
   * @return new default Bach instance
   */
  public static Bach of() {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    var configuration = Bach.Configuration.of(Path.of(""));
    return new Bach(out, err, configuration);
  }

  /**
   * Main entry-point of Bach.
   *
   * @param arguments task name(s) and their argument(s)
   * @throws Error on a non-zero error code
   */
  public static void main(String... arguments) {
    var args = List.of(Util.assigned(arguments, "arguments"));
    var bach = Bach.of();
    var code = bach.main(args);
    if (code != 0) {
      throw new Error("Bach.main(" + Util.join(arguments) + ") failed with error code: " + code);
    }
  }

  /** Text-output writer. */
  final PrintWriter out, err;
  /** Configuration. */
  final Configuration configuration;
  /** Tool caller. */
  final Runner runner;

  Bach(PrintWriter out, PrintWriter err, Configuration configuration) {
    this.out = Util.assigned(out, "out");
    this.err = Util.assigned(err, "err");
    this.configuration = Util.assigned(configuration, "configuration");
    this.runner = new Runner();
  }

  /** Log message unless threshold suppresses it. */
  public void log(System.Logger.Level level, String format, Object... args) {
    if (level.getSeverity() < configuration.basic.threshold.getSeverity()) {
      return;
    }
    var consumer = level.getSeverity() < WARNING.getSeverity() ? out : err;
    var message = String.format(format, args);
    consumer.println(message);
  }

  /** Main-entry point running tools indicated by the given arguments. */
  public int main(List<String> arguments) {
    log(INFO, "Bach.java " + VERSION + " building " + configuration.project);
    log(DEBUG, "  arguments=" + Util.assigned(arguments, "arguments"));
    log(DEBUG, "Configuration");
    log(DEBUG, "  tools=" + configuration.basic.tools);
    ServiceLoader.load(ToolProvider.class).forEach(it -> log(DEBUG, "  %-7s -> %s", it.name(), it));

    var deque = new ArrayDeque<>(arguments);
    while (!deque.isEmpty()) {
      var argument = deque.removeFirst();
      if ("tool".equals(argument)) {
        var name = deque.removeFirst();
        return runner.run(name, deque.toArray(Object[]::new));
      }
      var code = runner.run(argument);
      if (code != 0) {
        return code;
      }
    }
    return 0;
  }

  /** Run named tool with specified arguments asserting an expected error code. */
  public void run(int expected, String name, Object... arguments) {
    var code = runner.run(name, arguments);
    if (code != expected) {
      var message = "Tool %s(%s) returned %d, but expected %d";
      throw new AssertionError(String.format(message, name, Util.join(arguments), code, expected));
    }
  }

  /** Common properties. */
  static class Configuration {

    /** Supported property keys with default values and descriptions. */
    enum Property {
      /** Name of the project. */
      NAME("project", "Name of the project."),
      /**
       * Version of the project.
       *
       * @see Version#parse(String)
       */
      VERSION("1.0.0-SNAPSHOT", "Version of the project. Must be parse-able by " + Version.class),
      // Paths
      /** Path to directory containing all Java module sources. */
      PATH_SOURCES("src", "Path to directory containing all Java module sources."),
      // Default options for various tools
      /** List of modules to compile, or '*' indicating all modules. */
      OPTIONS_MODULES("*", "List of modules to compile, or '*' indicating all modules."),
      /** Options passed to all 'javac' calls. */
      OPTIONS_JAVAC("-encoding\nUTF-8\n-parameters\n-Xlint", "Options passed to 'javac' calls.");

      final String key;
      final String defaultValue;
      final String description;

      Property(String defaultValue, String description) {
        this.key = name().replace('_', '.').toLowerCase();
        this.defaultValue = defaultValue;
        this.description = description;
      }

      String get(Properties properties) {
        return get(properties, () -> defaultValue);
      }

      String get(Properties properties, Supplier<Object> defaultValueSupplier) {
        return Util.get(key, properties, defaultValueSupplier);
      }

      List<String> lines(Properties properties) {
        return get(properties).lines().collect(Collectors.toList());
      }
    }

    static class Basic {
      /** Current logging level threshold. */
      final System.Logger.Level threshold;
      /** Custom tool map. */
      final Map<String, Tool> tools;

      Basic(System.Logger.Level threshold, Map<String, Tool> tools) {
        this.threshold = threshold;
        this.tools = tools;
      }
    }

    /** Directories, files and other paths. */
    static class Paths {
      final Path home;
      final Path work;
      final Path sources;

      Paths(Path home, Path work, Path sources) {
        this.home = home;
        this.work = work;
        this.sources = sources;
      }
    }

    /** Default options passed to various tools. */
    static class Options {

      static List<String> modules(String modules, Path sources) {
        if ("*".equals(modules)) {
          return Util.findDirectoryNames(sources);
        }
        return List.of(modules.split(","));
      }

      final List<String> modules;
      final List<String> javac;

      Options(List<String> modules, List<String> javac) {
        this.modules = modules;
        this.javac = javac;
      }
    }

    /** Create project based on the given path, either a directory or a properties file. */
    public static Configuration of(Path path) {
      if (Files.isDirectory(path)) {
        var directory = Objects.toString(path.getFileName(), Property.NAME.defaultValue);
        for (var name : List.of(directory, "bach", "")) {
          var file = path.resolve(name + ".properties");
          if (Files.isRegularFile(file)) {
            return of(path, path, Util.loadProperties(file));
          }
        }
        return of(path, path, new Properties());
      }
      var home = Optional.ofNullable(path.getParent()).orElse(Path.of(""));
      return of(home, home, Util.loadProperties(path));
    }

    private static Configuration of(Path home, Path work, Properties properties) {
      // basics...
      var debug = System.getProperty("debug".substring(1)) != null;
      var basic = new Basic(debug ? ALL : INFO, Map.of());
      // project...
      var name = Property.NAME.get(properties, () -> home.toAbsolutePath().getFileName());
      var version = Version.parse(Property.VERSION.get(properties));
      var project = new Project(name, version);
      // paths...
      var sources = home.resolve(Property.PATH_SOURCES.get(properties));
      var paths = new Paths(home, work, sources);
      // options...
      var modules = Options.modules(Property.OPTIONS_MODULES.get(properties), sources);
      var javac = Property.OPTIONS_JAVAC.lines(properties);
      var options = new Options(modules, javac);

      return new Configuration(basic, project, paths, options);
    }

    final Basic basic;
    final Project project;
    final Paths paths;
    final Options options;

    Configuration(Basic basic, Project project, Paths paths, Options options) {
      this.basic = basic;
      this.project = project;
      this.paths = paths;
      this.options = options;
    }
  }

  /** Modular project model. */
  static class Project {
    final String name;
    final Version version;

    Project(String name, Version version) {
      this.name = name;
      this.version = version;
    }

    @Override
    public String toString() {
      return name + ' ' + version;
    }
  }

  /** Tool-invoking dispatcher. */
  class Runner {

    /** Run named tool with specified arguments returning an error code. */
    int run(String name, Object... arguments) {
      log(INFO, ">> %s(%s)%n", name, Util.join(arguments));
      var tool = configuration.basic.tools.get(name);
      if (tool != null) {
        log(DEBUG, "Calling tool named '%s'...", tool.name());
        return tool.run(arguments);
      }
      var provider = ToolProvider.findFirst(name);
      return provider
          .map(toolProvider -> toolProvider.run(out, err, Util.strings(arguments)))
          .orElse(42);
    }
  }

  /** Custom tool interface. */
  @FunctionalInterface
  public interface Tool {

    default String name() {
      return getClass().getSimpleName();
    }

    int run(Object... arguments);
  }

  /** Static helper. */
  static class Util {

    Util() {
      throw new Error();
    }

    /** Assigned returns P if P is non-nil and throws an exception if P is nil. */
    static <T> T assigned(T object, String name) {
      return Objects.requireNonNull(object, name + " must not be null");
    }

    /** List names of all directories found in given directory. */
    static List<String> findDirectoryNames(Path directory) {
      return findDirectoryEntries(directory, Files::isDirectory);
    }

    /** List paths of all entries found in given directory after applying the filter. */
    static List<String> findDirectoryEntries(Path directory, DirectoryStream.Filter<Path> filter) {
      var names = new ArrayList<String>();
      try (var stream = Files.newDirectoryStream(directory, filter)) {
        stream.forEach(entry -> names.add(entry.getFileName().toString()));
      } catch (IOException e) {
        throw new UncheckedIOException("Scanning directory entries failed: " + directory, e);
      }
      Collections.sort(names);
      return names;
    }

    /** Find native foundation tool, an executable program in given paths. */
    static Optional<Path> findExecutable(List<Path> paths, String name) {
      try {
        for (var path : paths) {
          for (var suffix : List.of("", ".exe")) {
            var program = path.resolve(name + suffix);
            if (Files.isRegularFile(program) && Files.isExecutable(program)) {
              return Optional.of(program);
            }
          }
        }
      } catch (InvalidPathException e) {
        // fall-through
      }
      return Optional.empty();
    }

    /** Gets a property value indicated by the specified {@code key}. */
    static String get(String key, Properties properties, Supplier<Object> defaultValueSupplier) {
      return Optional.ofNullable(System.getProperty(assigned(key, "key")))
          .or(() -> Optional.ofNullable(assigned(properties, "properties").getProperty(key)))
          .or(() -> Optional.ofNullable(System.getenv("BACH_" + key.toUpperCase())))
          .orElse(Objects.toString(assigned(defaultValueSupplier, "defaultValueSupplier").get()));
    }

    /** Join an array of objects into a human-readable string. */
    @SafeVarargs
    static <T> String join(T... objects) {
      var list = new ArrayList<String>();
      for (var object : objects) {
        list.add(Objects.toString(object, "<null>"));
      }
      return list.isEmpty() ? "<empty>" : '"' + String.join("\", \"", list) + '"';
    }

    /** Load specified properties file. */
    static Properties loadProperties(Path path) {
      var properties = new Properties();
      try (var reader = Files.newBufferedReader(path)) {
        properties.load(reader);
      } catch (IOException e) {
        throw new UncheckedIOException("Reading properties failed: " + path, e);
      }
      return properties;
    }

    /** Convert given array of objects to an array of strings. */
    static String[] strings(Object... objects) {
      var list = new ArrayList<String>();
      for (var object : objects) {
        list.add(Objects.toString(object, null));
      }
      return list.toArray(String[]::new);
    }
  }
}
