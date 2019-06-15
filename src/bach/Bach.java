// THIS FILE WAS GENERATED ON 2019-06-15T17:53:05.958622200Z
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
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.Logger.Level.WARNING;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
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
    run.log(DEBUG, "%s initialized", this);
    run.log(TRACE, "home = \"%s\"", run.home);
  }

  void help() {
    run.log(TRACE, "Bach::help()");
    run.out.println("Usage of Bach.java (" + VERSION + "):  java Bach.java [<action>...]");
    run.out.println("Available default actions are:");
    for (var action : Action.Default.values()) {
      var name = action.name().toLowerCase();
      var text =
          String.format(" %-9s    ", name) + String.join('\n' + " ".repeat(14), action.description);
      text.lines().forEach(run.out::println);
    }
  }

  /** Main entry-point, by convention, a zero status code indicates normal termination. */
  int main(List<String> arguments) {
    run.log(TRACE, "Bach::main(%s)", arguments);
    List<Action> actions;
    try {
      actions = Action.of(arguments);
      run.log(DEBUG, "actions = " + actions);
    } catch (IllegalArgumentException e) {
      run.log(ERROR, "Converting arguments to actions failed: " + e);
      return 1;
    }
    if (run.dryRun) {
      run.log(INFO, "Dry-run ends here.");
      return 0;
    }
    run(actions);
    return 0;
  }

  /** Execute a collection of actions sequentially on this instance. */
  int run(Collection<? extends Action> actions) {
    run.log(TRACE, "Bach::run(%s)", actions);
    run.log(DEBUG, "Performing %d action(s)...", actions.size());
    for (var action : actions) {
      try {
        run.log(TRACE, ">> %s", action);
        action.perform(this);
        run.log(TRACE, "<< %s", action);
      } catch (Exception exception) {
        run.log(ERROR, "Action %s threw: %s", action, exception);
        if (run.debug) {
          exception.printStackTrace(run.err);
        }
        return 1;
      }
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

  /** Bach consuming no-arg action operating via side-effects. */
  @FunctionalInterface
  public interface Action {

    /** Performs this action on the given Bach instance. */
    void perform(Bach bach) throws Exception;

    /** Transform a name and arguments into an action object. */
    static Action of(String name, Deque<String> arguments) {
      try {
        var actionClass = Class.forName(name);
        if (Action.class.isAssignableFrom(actionClass)) {
          return (Action) actionClass.getConstructor().newInstance();
        }
        throw new IllegalArgumentException(actionClass + " doesn't implement " + Action.class);
      } catch (ReflectiveOperationException e) {
        // fall-through
      }
      var defaultAction = Action.Default.valueOf(name.toUpperCase());
      return defaultAction.consume(arguments);
    }

    /** Transform strings to actions. */
    static List<Action> of(List<String> args) {
      var actions = new ArrayList<Action>();
      if (args.isEmpty()) {
        actions.add(Action.Default.HELP);
      } else {
        var arguments = new ArrayDeque<>(args);
        while (!arguments.isEmpty()) {
          var argument = arguments.removeFirst();
          actions.add(of(argument, arguments));
        }
      }
      return actions;
    }

    /** Default action delegating to Bach API methods. */
    enum Default implements Action {
      // BUILD(Bach::build, "Build modular Java project in base directory."),
      // CLEAN(Bach::clean, "Delete all generated assets - but keep caches intact."),
      // ERASE(Bach::erase, "Delete all generated assets - and also delete caches."),
      HELP(Bach::help, "Print this help screen on standard out... F1, F1, F1!"),
      // LAUNCH(Bach::launch, "Start project's main program."),
      TOOL(
          null,
          "Run named tool consuming all remaining arguments:",
          "  tool <name> <args...>",
          "  tool java --show-version Program.java") {
        /** Return new Action running the named tool and consuming all remaining arguments. */
        @Override
        Action consume(Deque<String> arguments) {
          var name = arguments.removeFirst();
          var args = arguments.toArray(String[]::new);
          arguments.clear();
          return bach -> bach.run.run(name, args);
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
        //        var key = "bach.action." + name().toLowerCase() + ".enabled";
        //        var enabled = Boolean.parseBoolean(bach.get(key, "true"));
        //        if (!enabled) {
        //          bach.run.info("Action " + name() + " disabled.");
        //          return;
        //        }
        action.perform(bach);
      }

      /** Return this default action instance without consuming any argument. */
      Action consume(Deque<String> arguments) {
        return this;
      }
    }
  }

  /** Build, i.e. compile and package, a modular Java project. */
  public static class JigsawBuilder implements Action {

    @Override
    public void perform(Bach bach) throws Exception {
      var home = bach.run.home;
      var work = home.resolve("target/bach"); // TODO bach.run.work...
      var lib = home.resolve("lib");
      var src = home.resolve("src");

      var libTest = lib.resolve("test");
      var libTestJUnitPlatform = lib.resolve("test-junit-platform");
      var libTestRuntimeOnly = lib.resolve("test-runtime-only");
      var sourceMainResources = src.resolve(Path.of("modules", "de.sormuras.bach", "main/resources"));
      var sourceTestResources = src.resolve(Path.of("modules", "de.sormuras.bach", "test/resources"));
      var targetMainClasses = work.resolve("main/classes");
      var targetMainModules = Files.createDirectories(work.resolve("main/modules"));
      var targetTestClasses = work.resolve("test/classes");
      var targetTestModules = Files.createDirectories(work.resolve("test/modules"));

      bach.run.run(
          new Command("javac")
              .add("-d", targetMainClasses)
              .add("--module-source-path", "src/modules/*/main/java")
              .add("--module-version", "1-" + bach.project.version)
              .add("--module", "de.sormuras.bach"));

      bach.run.run(
          new Command("jar")
              .add("--create")
              .addIff(bach.run.debug, "--verbose")
              .add("--file", targetMainModules.resolve("de.sormuras.bach.jar"))
              .add("-C", targetMainClasses.resolve("de.sormuras.bach"))
              .add(".")
              .addIff(
                  Files.isDirectory(sourceMainResources),
                  cmd -> cmd.add("-C", sourceMainResources).add(".")));

      bach.run.run(
          new Command("javac")
              .add("-d", targetTestClasses)
              .add("--module-source-path", "src/modules/*/test/java")
              .add("--module-version", "1-" + bach.project.version)
              .add("--module-path", List.of(targetMainModules, libTest))
              .add("--module", "integration"));

      bach.run.run(
          new Command("jar")
              .add("--create")
              .addIff(bach.run.debug, "--verbose")
              .add("--file", targetTestModules.resolve("integration.jar"))
              .add("-C", targetTestClasses.resolve("integration"))
              .add(".")
              .addIff(
                  Files.isDirectory(sourceTestResources),
                  cmd -> cmd.add("-C", sourceTestResources).add(".")));

      var test =
          new Command("java")
              .add(
                  "--module-path",
                  List.of(targetTestModules, targetMainModules, libTest, libTestJUnitPlatform, libTestRuntimeOnly))
              .add("--add-modules", "integration")
              .add("--module", "org.junit.platform.console")
              .add("--scan-modules");
      bach.run.log(INFO, "%s %s", test.name, String.join(" ", test.list));
    }
  }
}
