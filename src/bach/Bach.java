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

import java.lang.System.Logger.Level;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** Java Shell Builder. */
class Bach {

  /** Version is either {@code master} or {@link Runtime.Version#parse(String)}-compatible. */
  public static final String VERSION = "master";

  /** Convenient short-cut to {@code "user.home"} as a path. */
  static final Path USER_HOME = Path.of(System.getProperty("user.home"));

  /** Convenient short-cut to {@code "user.dir"} as a path. */
  static final Path USER_PATH = Path.of(System.getProperty("user.dir"));

  /** Main entry-point throwing runtime exception on error. */
  public static void main(String... args) {
    var bach = new Bach();
    var actions = bach.actions(args);
    bach.run(actions);
  }

  /** {@code -Debug=true} flag. */
  final boolean debug;

  /** Base path defaults to user's current working directory. */
  final Path base;

  /** Logging helper. */
  final Log log;

  /** Project model. */
  final Project project;

  /** User-defined properties loaded from {@code ${base}/bach.properties} file. */
  final Properties properties;

  /** Initialize Bach instance using system properties. */
  Bach() {
    this(Boolean.getBoolean("ebug"), Path.of(Property.BASE.get()));
  }

  /** Initialize Bach instance in supplied working directory. */
  Bach(boolean debug, Path base) {
    this.debug = debug;
    this.base = base.normalize();
    this.properties = Property.loadProperties(base.resolve(Property.PROPERTIES.get()));
    this.log = new Log();
    this.project = new Project();
  }

  /** Transforming strings to actions. */
  List<Action> actions(String... args) {
    var actions = new ArrayList<Action>();
    if (args.length == 0) {
      actions.add(Action.Default.BUILD);
    } else {
      var arguments = new ArrayDeque<>(List.of(args));
      while (!arguments.isEmpty()) {
        var argument = arguments.removeFirst();
        var defaultAction = Action.Default.valueOf(argument.toUpperCase());
        var action = defaultAction.consume(arguments);
        actions.add(action);
      }
    }
    return actions;
  }

  /** Build all and everything. */
  public void build() throws Exception {
    project.main.compile();
    project.test.compile();
  }

  /** Delete generated binary assets. */
  public void clean() throws Exception {
    Util.treeDelete(project.bin);
  }

  /** Delete generated binary assets and local build cache directory. */
  public void erase() throws Exception {
    clean();
    Util.treeDelete(project.cache);
  }

  /** Gets the property value. */
  String get(Property property) {
    return get(property.key, property.defaultValue);
  }

  /** Gets the property value indicated by the specified key. */
  String get(String key, String defaultValue) {
    var value = System.getProperty(key);
    if (value != null) {
      log.log(Level.TRACE, String.format("Got system property %s: %s", key, value));
      return value;
    }
    return properties.getProperty(key, defaultValue);
  }

  /** Print help text to the "standard" output stream. */
  public void help() {
    System.out.println();
    help(System.out::println);
    System.out.println();
  }

  /** Print help text to given print stream. */
  void help(Consumer<String> out) {
    out.accept("Usage of Bach.java (" + VERSION + "):  java Bach.java [<action>...]");
    out.accept("Available default actions are:");
    for (var action : Action.Default.values()) {
      var name = action.name().toLowerCase();
      var text =
          String.format(" %-9s    ", name) + String.join('\n' + " ".repeat(14), action.description);
      text.lines().forEach(out);
    }
  }

  /** Execute a collection of actions sequentially on this instance. */
  void run(Collection<? extends Action> actions) {
    log.log(Level.DEBUG, String.format("Performing %d action(s)...", actions.size()));
    for (var action : actions) {
      try {
        log.log(Level.TRACE, String.format(">> %s", action));
        action.perform(this);
        log.log(Level.TRACE, String.format("<< %s", action));
      } catch (Throwable throwable) {
        log.log(Level.ERROR, throwable.getMessage());
        throw new Error("Action failed: " + action, throwable);
      }
    }
  }

  /** Execute the named tool and throw an error the expected and actual exit values aren't equal. */
  void run(int expected, String name, Object... arguments) {
    var actual = run(name, arguments);
    if (expected != actual) {
      throw new Error(name + " returned " + actual + ", but expected " + expected);
    }
  }

  /** Execute the named tool and return its exit value. */
  int run(String name, Object... arguments) {
    var args = new String[arguments.length];
    for (int i = 0; i < args.length; i++) {
      args[i] = arguments[i].toString();
    }
    log.log(Level.DEBUG, String.format("run(%s, %s)", name, List.of(args)));
    var toolProvider = ToolProvider.findFirst(name);
    if (toolProvider.isPresent()) {
      var tool = toolProvider.get();
      log.log(Level.DEBUG, "Running provided tool in-process: " + tool);
      return tool.run(System.out, System.err, args);
    }
    // TODO Find registered tool, like "format", "junit", "maven", "gradle"
    // TODO Find executable via {java.home}/${name}[.exe]
    try {
      var builder = new ProcessBuilder(name).inheritIO();
      builder.command().addAll(List.of(args));
      var process = builder.start();
      log.log(Level.DEBUG, "Running tool in a new process: " + process);
      return process.waitFor();
    } catch (Exception e) {
      throw new Error("Running tool " + name + " failed!", e);
    }
  }

  /** Bach consuming action operating via side-effects. */
  @FunctionalInterface
  interface Action {

    /** Performs this action on the given Bach instance. */
    void perform(Bach bach) throws Exception;

    /** Default action delegating to Bach API methods. */
    enum Default implements Action {
      BUILD(Bach::build, "Build modular Java project"),
      CLEAN(Bach::clean, "Delete all generated assets - but keep caches intact."),
      ERASE(Bach::erase, "Delete all generated assets - and also delete caches."),
      HELP(Bach::help, "Print this help screen on standard out... F1, F1, F1!"),
      TOOL(
          null,
          "Run named tool consuming all remaining arguments",
          "  tool <name> <args...>",
          "  tool java --show-version Program.java") {
        /** Return new Action running the named tool and consuming all remaining arguments. */
        @Override
        Action consume(Deque<String> arguments) {
          var name = arguments.removeFirst();
          var args = arguments.toArray(Object[]::new);
          arguments.clear();
          return bach -> bach.run(name, args);
        }
      };

      final Action action;
      final String[] description;

      Default(Action action, String... description) {
        this.action = action;
        this.description = description;
      }

      @Override
      public void perform(Bach bach) throws Exception {
        var key = "bach.action." + name().toLowerCase() + ".enabled";
        var enabled = Boolean.parseBoolean(bach.get(key, "true"));
        if (!enabled) {
          bach.log.log(Level.INFO, "Action " + name() + " disabled.");
          return;
        }
        action.perform(bach);
      }

      /** Return this default action instance without consuming any argument. */
      Action consume(Deque<String> arguments) {
        return this;
      }
    }
  }

  /** Property names, keys and default values. */
  enum Property {
    PROPERTIES("bach.properties"),
    BASE("."),
    LOG_LEVEL("INFO");

    /** Load properties from given path. */
    static Properties loadProperties(Path path) {
      var properties = new Properties();
      if (Files.exists(path)) {
        try (var stream = Files.newInputStream(path)) {
          properties.load(stream);
        } catch (Exception e) {
          throw new Error("Loading properties failed: " + path, e);
        }
      }
      return properties;
    }

    final String key;
    final String defaultValue;

    Property(String defaultValue) {
      this.key = "bach." + name().toLowerCase().replace('_', '.');
      this.defaultValue = defaultValue;
    }

    String get() {
      return System.getProperty(key, defaultValue);
    }
  }

  /** Logging helper. */
  final class Log {

    /** Current logging level threshold. */
    Level threshold = debug ? Level.ALL : Level.valueOf(get(Property.LOG_LEVEL));

    /** Standard output message consumer. */
    Consumer<String> out = System.out::println;

    /** Error output stream. */
    Consumer<String> err = System.err::println;

    /** Log message unless threshold suppresses it. */
    void log(Level level, String message) {
      if (level.getSeverity() < threshold.getSeverity()) {
        return;
      }
      var consumer = level.getSeverity() < Level.WARNING.getSeverity() ? out : err;
      consumer.accept(message);
    }
  }

  /** Project model. */
  final class Project {
    /** Destination directory for generated binaries. */
    final Path bin;
    /** Root of local build cache. */
    final Path cache;
    /** Name of the project. */
    final String name;
    /** Main realm. */
    final Realm main;
    /** Test realm. */
    final Realm test;

    /** Initialize project properties with default values. */
    Project() {
      this.bin = base.resolve("bin");
      this.cache = base.resolve(".bach");
      this.name = base.getNameCount() > 0 ? base.toAbsolutePath().getFileName() + "" : "project";
      this.main = new Realm("main", List.of("src/main/java", "src/main", "main", "src"));
      this.test = new Realm("test", List.of("src/test/java", "src/test", "test"));
    }

    /** Rebase path as needed. */
    Path based(Path path) {
      if (path.isAbsolute()) {
        return path;
      }
      if (base.toAbsolutePath().equals(USER_PATH)) {
        return path;
      }
      return base.resolve(path).normalize();
    }

    /** Create and rebase path as needed. */
    Path based(String first, String... more) {
      return based(Path.of(first, more));
    }

    /** List all regular files matching the given filter. */
    List<Path> findFiles(Collection<Path> roots, Predicate<Path> filter) throws Exception {
      var files = new ArrayList<Path>();
      for (var root : roots) {
        try (var stream = Files.walk(root)) {
          stream.filter(Files::isRegularFile).filter(filter).forEach(files::add);
        }
      }
      return files;
    }

    /** Building block, source set, scope, directory, named context: {@code main}, {@code test}. */
    class Realm {
      /** Name of the realm. */
      final String name;
      /** Source path. */
      final Path source;
      /** Target path. */
      final Path target;

      /** Initialize this realm. */
      Realm(String name, List<String> sources) {
        this.name = name;
        this.source =
            sources.stream()
                .map(Project.this::based)
                .filter(Files::isDirectory)
                .findFirst()
                .orElse(based(sources.get(0)));
        this.target = bin.resolve(Path.of("realm", name));
      }

      /** Compile all Java sources found in this realm. */
      void compile() throws Exception {
        log.log(Level.TRACE, String.format("%s.compile()", name));
        if (Files.notExists(source)) {
          log.log(Level.INFO, String.format("Skip %s.compile(): path %s not found", name, source));
          return;
        }
        var javac = new ArrayList<>();
        javac.add("-d");
        javac.add(target);
        // javac.add("--module-path").add(modules);
        javac.add("--module-source-path");
        javac.add(source);
        javac.addAll(findFiles(List.of(source), Util::isJavaFile));
        run(0, "javac", javac.toArray(Object[]::new));
      }
    }
  }

  /** Static helpers. */
  static final class Util {
    /** No instance permitted. */
    Util() {
      throw new Error();
    }

    /** Test supplied path for pointing to a Java source compilation unit. */
    static boolean isJavaFile(Path path) {
      if (Files.isRegularFile(path)) {
        var name = path.getFileName().toString();
        if (name.endsWith(".java")) {
          return name.indexOf('.') == name.length() - 5; // single dot in filename
        }
      }
      return false;
    }

    /** Delete all files and directories from and including the root directory. */
    static void treeDelete(Path root) throws Exception {
      treeDelete(root, __ -> true);
    }

    /** Delete selected files and directories from and including the root directory. */
    static void treeDelete(Path root, Predicate<Path> filter) throws Exception {
      // trivial case: delete existing empty directory or single file
      if (filter.test(root)) {
        try {
          Files.deleteIfExists(root);
          return;
        } catch (DirectoryNotEmptyException ignored) {
          // fall-through
        }
      }
      // default case: walk the tree...
      try (var stream = Files.walk(root)) {
        var selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
        for (var path : selected.collect(Collectors.toList())) {
          Files.deleteIfExists(path);
        }
      }
    }
  }
}
