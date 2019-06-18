// THIS FILE WAS GENERATED ON 2019-06-18T09:49:55.990707Z
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
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** Java Shell Builder. */
public class Bach {

  /** Version of Bach, {@link Runtime.Version#parse(String)}-compatible. */
  public static final String VERSION = "2-ea";

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

  final Run run;
  final Project project;

  public Bach() {
    this(Run.system());
  }

  public Bach(Run run) {
    this(run, Project.of(run.home));
  }

  public Bach(Run run, Project project) {
    this.run = run;
    this.project = project;
    run.log(DEBUG, "%s initialized for %s", this, project);
    project.toStrings(line -> run.log(DEBUG, "  %s", line));
    run.log(TRACE, "Run instance properties");
    run.log(TRACE, "  class = %s", run.getClass().getSimpleName());
    run.toStrings(line -> run.log(TRACE, "  %s", line));
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
    run.out.println("Project information");
    project.toStrings(run.out::println);
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
    run.log(DEBUG, "%s action(s) successfully performed.", actions.size());
    return 0;
  }

  /** Resolve required external assets, like 3rd-party modules. */
  void synchronize() throws Exception {
    run.log(TRACE, "Bach::synchronize()");
    synchronizeModuleUriProperties(run.home.resolve("lib"));
    // TODO synchronizeMissingLibrariesByParsingModuleDescriptors();
  }

  private void synchronizeModuleUriProperties(Path root) throws Exception {
    run.log(DEBUG, "Synchronizing 3rd-party module uris below %s", root);
    if (Files.notExists(root)) {
      run.log(DEBUG, "Directory %s doesn't exist, not synchronizing.", root);
      return;
    }
    var paths = new ArrayList<Path>();
    try (var stream = Files.walk(root)) {
      stream
          .filter(path -> path.getFileName().toString().equals("module-uri.properties"))
          .forEach(paths::add);
    }
    var synced = new ArrayList<Path>();
    for (var path : paths) {
      var directory = path.getParent();
      var downloader = new Downloader(run, directory);
      var properties = new Properties();
      try (var stream = Files.newInputStream(path)) {
        properties.load(stream);
      }
      if (properties.isEmpty()) {
        run.log(DEBUG, "No module uri declared in %s", path.toUri());
        continue;
      }
      run.log(DEBUG, "Syncing %d module uri(s) to %s", properties.size(), directory.toUri());
      for (var value : properties.values()) {
        var string = run.replaceVariables(value.toString());
        var uri = URI.create(string);
        uri = uri.isAbsolute() ? uri : run.home.resolve(string).toUri();
        run.log(DEBUG, "Syncing %s", uri);
        var target = downloader.download(uri);
        synced.add(target);
        run.log(DEBUG, " o %s", target.toUri());
      }
    }
    run.log(DEBUG, "Synchronized %d module uri(s).", synced.size());
  }

  @Override
  public String toString() {
    return "Bach (" + VERSION + ")";
  }

  /** Bach consuming no-arg action operating via side-effects. */
  @FunctionalInterface
  public interface Action {

    /** Performs this action on the given Bach instance. */
    void perform(Bach bach) throws Exception;

    /** Transform a name and arguments into an action object. */
    static Action of(String name, Deque<String> arguments) {
      // try {
      //   var method = Bach.class.getMethod(name);
      //   return bach -> method.invoke(bach);
      // } catch (ReflectiveOperationException e) {
      //   // fall-through
      // }
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
      SYNC(Bach::synchronize, "Resolve required external assets, like 3rd-party modules."),
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

  /** Downloader. */
  public static class Downloader {

    /** Extract last path element from the supplied uri. */
    static String extractFileName(URI uri) {
      var path = uri.getPath(); // strip query and fragment elements
      return path.substring(path.lastIndexOf('/') + 1);
    }

    /** Extract target file name either from 'Content-Disposition' header or. */
    static String extractFileName(URLConnection connection) throws Exception {
      var contentDisposition = connection.getHeaderField("Content-Disposition");
      if (contentDisposition != null && contentDisposition.indexOf('=') > 0) {
        return contentDisposition.split("=")[1].replaceAll("\"", "");
      }
      return extractFileName(connection.getURL().toURI());
    }

    final Run run;
    final Path destination;

    Downloader(Run run, Path destination) {
      this.run = run;
      this.destination = destination;
    }

    /** Download a file denoted by the specified uri. */
    Path download(URI uri) throws Exception {
      run.log(TRACE, "Downloader::download(%s)", uri);
      var fileName = extractFileName(uri);
      var target = Files.createDirectories(destination).resolve(fileName);
      var url = uri.toURL(); // fails for non-absolute uri
      if (run.isOffline()) {
        run.log(DEBUG, "Offline mode is active!");
        if (Files.exists(target)) {
          var file = target.getFileName().toString();
          run.log(DEBUG, "Target already exists: %s, %d bytes.", file, Files.size(target));
          return target;
        }
        var message = "Offline mode is active and target is missing: " + target;
        run.log(ERROR, message);
        throw new IllegalStateException(message);
      }
      return download(url.openConnection());
    }

    /** Download a file using the given URL connection. */
    Path download(URLConnection connection) throws Exception {
      var millis = connection.getLastModified(); // 0 means "unknown"
      var lastModified = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
      run.log(TRACE, "Remote was modified on %s", lastModified);
      var target = destination.resolve(extractFileName(connection));
      run.log(TRACE, "Local target file is %s", target.toUri());
      var file = target.getFileName().toString();
      if (Files.exists(target)) {
        var fileModified = Files.getLastModifiedTime(target);
        run.log(TRACE, "Local last modified on %s", fileModified);
        if (fileModified.equals(lastModified)) {
          run.log(TRACE, "Timestamp match: %s, %d bytes.", file, Files.size(target));
          connection.getInputStream().close(); // release all opened resources
          return target;
        }
        run.log(DEBUG, "Local target file differs from remote source -- replacing it...");
      }
      try (var sourceStream = connection.getInputStream()) {
        try (var targetStream = Files.newOutputStream(target)) {
          run.log(DEBUG, "Transferring %s...", file);
          sourceStream.transferTo(targetStream);
        }
        Files.setLastModifiedTime(target, lastModified);
      }
      run.log(DEBUG, "Downloaded %s, %d bytes.", file, Files.size(target));
      return target;
    }
  }

  /** Build, i.e. compile and package, a modular Java project. */
  public static class JigsawBuilder implements Action {

    static List<String> modules(Project project, String realm) {
      var userDefinedModules = project.get(Project.Property.MODULES);
      if (!userDefinedModules.equals("*")) {
        return List.of(userDefinedModules.split("\\s*,\\s*"));
      }
      // Find modules for realm...
      var modules = new ArrayList<String>();
      var descriptor = Path.of(realm, "java", "module-info.java");
      DirectoryStream.Filter<Path> filter =
          path -> Files.isDirectory(path) && Files.exists(path.resolve(descriptor));
      try (var stream = Files.newDirectoryStream(project.path(Project.Property.PATH_SRC), filter)) {
        stream.forEach(directory -> modules.add(directory.getFileName().toString()));
      } catch (Exception e) {
        throw new Error(e);
      }
      return modules;
    }

    @Override
    public void perform(Bach bach) throws Exception {
      var worker = new Worker(bach);
      worker.compile("main");
    }

    static class Worker {
      final Bach bach;
      final Path bin;
      final Path lib;
      final Path src;
      final String version;

      Worker(Bach bach) {
        this.bach = bach;
        this.version = bach.project.version;
        this.bin = bach.run.work.resolve(bach.project.path(Project.Property.PATH_BIN));
        this.lib = bach.run.home.resolve(bach.project.path(Project.Property.PATH_LIB));
        this.src = bach.run.home.resolve(bach.project.path(Project.Property.PATH_SRC));
      }

      void compile(String realm) throws Exception {
        var modules = modules(bach.project, realm);
        compile(realm, modules);
      }

      void compile(String realm, List<String> modules) throws Exception {
        bach.run.log(DEBUG, "Compiling %s modules: %s", realm, modules);
        var classes = bin.resolve(realm + "/classes");

        bach.run.run(
            new Command("javac")
                .add("-d", classes)
                .add("--module-source-path", src + "/*/" + realm + "/java")
                .add("--module-version", version)
                .add("--module", String.join(",", modules)));

        var jars = Files.createDirectories(bin.resolve(realm + "/modules"));
        for (var module : modules) {
          var resources = src.resolve(Path.of(module, realm, "resources"));
          bach.run.run(
              new Command("jar")
                  .add("--create")
                  .addIff(bach.run.debug, "--verbose")
                  .add("--file", jars.resolve(module + '-' + version + ".jar"))
                  .add("-C", classes.resolve(module))
                  .add(".")
                  .addIff(Files.isDirectory(resources), cmd -> cmd.add("-C", resources).add(".")));
        }
      }
    }
  }

  /** Project data. */
  public static class Project {

    /** Project property enumeration. */
    public enum Property {
      NAME(
          "unnamed",
          "Name of the project. Determines some file names, like main documentation JAR file."),
      VERSION(
          "1.0.0-SNAPSHOT",
          "Version of the project. Passed to '--module-version' and other options."),
      MODULES("*", "List of modules to build. '*' means all in PATH_SRC_MODULES."),
      PATH_BIN("bin", "Destination directory to store binary assets to."),
      PATH_LIB("lib", "Root directory of 3rd-party modules."),
      PATH_SRC("src", "This directory contains all Java module sources."),
      ;

      final String key;
      final String defaultValue;
      final String description;

      Property(String defaultValue, String description) {
        this.key = name().toLowerCase().replace('_', '.');
        this.defaultValue = defaultValue;
        this.description = description;
      }

      String get(Properties properties) {
        return get(properties, defaultValue);
      }

      String get(Properties properties, String defaultValue) {
        return properties.getProperty(key, defaultValue);
      }
    }

    public static Project of(Path home) {
      var homeName = "" + home.toAbsolutePath().normalize().getFileName();
      return new Project(Run.newProperties(home), homeName);
    }

    final Properties properties;
    final String name;
    final String version;

    private Project(Properties properties, String defaultName) {
      this.properties = properties;
      this.name = Property.NAME.get(properties, defaultName);
      this.version = Property.VERSION.get(properties);
    }

    String get(Property property) {
      return property.get(properties);
    }

    Path path(Property property) {
      return Path.of(get(property));
    }

    @Override
    public String toString() {
      return name + ' ' + version;
    }

    void toStrings(Consumer<String> consumer) {
      var skips = Set.of("name", "version");
      consumer.accept("name = " + name);
      consumer.accept("version = " + version);
      for (var property : Property.values()) {
        var key = property.key;
        if (skips.contains(key)) {
          continue;
        }
        consumer.accept(key + " = " + get(property));
      }
    }
  }

  /** Runtime context information. */
  public static class Run {

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
}
