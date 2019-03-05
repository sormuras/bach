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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;

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

  /** Initialize Bach instance using system properties. */
  Bach() {
    this(Boolean.getBoolean("ebug"), Path.of(System.getProperty("bach.base", "")));
  }

  /** Initialize Bach instance in supplied working directory. */
  Bach(boolean debug, Path base) {
    this.debug = debug;
    this.base = base.normalize();
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
    for (var action : Bach.Action.Default.values()) {
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
      HELP(Bach::help, "Print this help screen on standard out... F1, F1, F1!"),
      TOOL(
          null,
          "Run named tool consuming all remaining arguments",
          "  tool <name> <args...>",
          "  tool java --show-version Program.java") {
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
        action.perform(bach);
      }

      Action consume(Deque<String> arguments) {
        return this;
      }
    }
  }

  /** Logging helper. */
  class Log {

    /** Current logging level threshold. */
    Level threshold = debug ? Level.ALL : Level.INFO;

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
  class Project {
    /** Destination directory for generated binaries. */
    final Path bin;
    /** Name of the project. */
    final String name;
    /** Main realm. */
    final Realm main;
    /** Test realm. */
    final Realm test;

    /** Initialize project properties with default values. */
    Project() {
      this.bin = base.resolve("bin");
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
        javac.addAll(findFiles(List.of(source), path -> path.toString().endsWith(".java")));
        run(0, "javac", javac.toArray(Object[]::new));
      }
    }
  }
}
