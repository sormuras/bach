// THIS FILE WAS GENERATED ON 2019-06-21T21:20:19.651540900Z
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

import static java.lang.ModuleLayer.defineModulesWithOneLoader;
import static java.lang.System.Logger.Level.ALL;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.Logger.Level.WARNING;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
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
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
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
    this(run, Project.of(run.home, run.work));
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

  /** Build project. */
  void build() throws Exception {
    run.log(TRACE, "Bach::build()");
    synchronize();
    new JigsawBuilder(this).build();
    new JUnitPlatformLauncher(this).call();
  }

  /** Print help message with project information section. */
  void help() {
    run.log(TRACE, "Bach::help()");
    run.out.println("Usage of Bach.java (" + VERSION + "):  java Bach.java [<task>...]");
    run.out.println("Available default tasks are:");
    for (var task : Task.Default.values()) {
      var name = task.name().toLowerCase();
      var text =
          String.format(" %-9s    ", name) + String.join('\n' + " ".repeat(14), task.description);
      text.lines().forEach(run.out::println);
    }
    run.out.println("Project information");
    project.toStrings(run.out::println);
  }

  /** Main entry-point, by convention, a zero status code indicates normal termination. */
  int main(List<String> arguments) {
    run.log(TRACE, "Bach::main(%s)", arguments);
    List<Task> tasks;
    try {
      tasks = Task.of(arguments);
      run.log(DEBUG, "tasks = " + tasks);
    } catch (IllegalArgumentException e) {
      run.log(ERROR, "Converting arguments to tasks failed: " + e);
      return 1;
    }
    if (run.dryRun) {
      run.log(INFO, "Dry-run ends here.");
      return 0;
    }
    run(tasks);
    return 0;
  }

  /** Execute a collection of tasks sequentially on this instance. */
  int run(Collection<? extends Task> tasks) {
    run.log(TRACE, "Bach::run(%s)", tasks);
    run.log(DEBUG, "Performing %d task(s)...", tasks.size());
    for (var task : tasks) {
      try {
        run.log(TRACE, ">> %s", task);
        task.perform(this);
        run.log(TRACE, "<< %s", task);
      } catch (Exception exception) {
        run.log(ERROR, "Task %s threw: %s", task, exception);
        if (run.debug) {
          exception.printStackTrace(run.err);
        }
        return 1;
      }
    }
    run.log(DEBUG, "%s task(s) successfully performed.", tasks.size());
    return 0;
  }

  /** Resolve required external assets, like 3rd-party modules. */
  void synchronize() throws Exception {
    run.log(TRACE, "Bach::synchronize()");
    if (run.isOffline()) {
      run.log(INFO, "Offline mode is active, no synchronization.");
      return;
    }
    synchronizeModuleUriProperties(project.lib);
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

  /** Launch the JUnit Platform Console using compiled test modules. */
  public static class JUnitPlatformLauncher implements Callable<Integer> {

    final Bach bach;
    final Run run;
    final Path bin;
    final Path lib;
    final String version;

    JUnitPlatformLauncher(Bach bach) {
      this.bach = bach;
      this.run = bach.run;
      this.version = bach.project.version;
      this.bin = bach.run.work.resolve(bach.project.path(Project.Property.PATH_BIN));
      this.lib = bach.run.home.resolve(bach.project.path(Project.Property.PATH_LIB));
    }

    @Override
    public Integer call() throws Exception {
      var modules = bach.project.modules("test");
      if (modules.isEmpty()) {
        return 0;
      }
      junit();
      bach.run.log(DEBUG, "JUnit successful.");
      return 0;
    }

    /** Launch JUnit Platform for given modular realm. */
    private void junit() throws Exception {
      var junit =
          new Command("junit")
              .add("--fail-if-no-tests")
              .add("--reports-dir", bin.resolve("test/junit-reports"))
              .add("--scan-modules");
      try {
        launchJUnitPlatformConsole(junit);
      } finally {
        var windows = System.getProperty("os.name", "?").toLowerCase(Locale.ENGLISH).contains("win");
        if (windows) {
          System.gc();
          Thread.sleep(1234);
        }
      }
    }

    /** Launch JUnit Platform for given modular realm. */
    private void launchJUnitPlatformConsole(Command junit) {
      var modulePath = bach.project.modulePath("test", "runtime", "main");
      run.log(DEBUG, "Module path:");
      for (var element : modulePath) {
        run.log(DEBUG, "  -> %s", element);
      }
      var finder = ModuleFinder.of(modulePath.toArray(Path[]::new));
      run.log(DEBUG, "Finder finds module(s):");
      for (var reference : finder.findAll()) {
        run.log(DEBUG, "  -> %s", reference);
      }
      var rootModules = new ArrayList<>(bach.project.modules("test"));
      rootModules.add("org.junit.platform.console");
      run.log(DEBUG, "Root module(s):");
      for (var module : rootModules) {
        run.log(DEBUG, "  -> %s", module);
      }
      var boot = ModuleLayer.boot();
      var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), rootModules);
      var parentLoader = ClassLoader.getPlatformClassLoader();
      var controller = defineModulesWithOneLoader(configuration, List.of(boot), parentLoader);
      var junitConsoleLayer = controller.layer();
      controller.addExports( // "Bach.java" resides in an unnamed module...
          junitConsoleLayer.findModule("org.junit.platform.console").orElseThrow(),
          "org.junit.platform.console",
          Bach.class.getModule());
      var junitConsoleLoader = junitConsoleLayer.findLoader("org.junit.platform.console");
      var junitLoader = new URLClassLoader("junit", new URL[0], junitConsoleLoader);
      launchJUnitPlatformConsole(run, junitLoader, junit);
    }

    private void launchJUnitPlatformConsole(Run run, ClassLoader loader, Command junit) {
      run.log(DEBUG, "__CHECK__");
      run.log(DEBUG, "Launching JUnit Platform Console: %s", junit.list);
      run.log(DEBUG, "Using class loader: %s - %s", loader.getName(), loader);
      var currentThread = Thread.currentThread();
      var currentContextLoader = currentThread.getContextClassLoader();
      currentThread.setContextClassLoader(loader);
      var parent = loader;
      while (parent != null) {
        parent.setDefaultAssertionStatus(true);
        parent = parent.getParent();
      }
      try {
        var launcher = loader.loadClass("org.junit.platform.console.ConsoleLauncher");
        var params = new Class<?>[] {PrintStream.class, PrintStream.class, String[].class};
        var execute = launcher.getMethod("execute", params);
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        var args = junit.toStringArray();
        var result = execute.invoke(null, new PrintStream(out), new PrintStream(err), args);
        run.out.write(out.toString());
        run.out.flush();
        run.err.write(err.toString());
        run.err.flush();
        var code = (int) result.getClass().getMethod("getExitCode").invoke(result);
        if (code != 0) {
          throw new AssertionError("JUnit run exited with code " + code);
        }
      } catch (Throwable t) {
        throw new Error("ConsoleLauncher.execute(...) failed: " + t, t);
      } finally {
        currentThread.setContextClassLoader(currentContextLoader);
      }
    }
  }

  /** Build, i.e. compile and package, a modular Java project. */
  public static class JigsawBuilder {

    final Bach bach;
    final Path bin;
    final Path lib;
    final Path src;
    final String version;

    JigsawBuilder(Bach bach) {
      this.bach = bach;
      this.version = bach.project.version;
      this.bin = bach.run.work.resolve(bach.project.path(Project.Property.PATH_BIN));
      this.lib = bach.run.home.resolve(bach.project.path(Project.Property.PATH_LIB));
      this.src = bach.run.home.resolve(bach.project.path(Project.Property.PATH_SRC));
    }

     void build() throws Exception {
      compile("main");
      compile("test", "main");
      bach.run.log(DEBUG, "Build successful.");
    }

    void compile(String realm, String... requiredRealms) throws Exception {
      var modules = bach.project.modules(realm);
      if (modules.isEmpty()) {
        bach.run.log(DEBUG, "No %s modules found.", realm);
        return;
      }
      compile(realm, modules, requiredRealms);
    }

    void compile(String realm, List<String> modules, String... requiredRealms) throws Exception {
      bach.run.log(DEBUG, "Compiling %s modules: %s", realm, modules);

      var classes = bin.resolve(realm + "/classes");
      var jars = Files.createDirectories(bin.resolve(realm + "/modules")); // "own" jars

      var modulePath = new ArrayList<>(bach.project.modulePath(realm, "compile", requiredRealms));
      modulePath.add(jars);
      for (var requiredRealm : requiredRealms) {
        modulePath.add(bin.resolve(requiredRealm + "/modules"));
      }

      bach.run.run(
          new Command("javac")
              .add("-d", classes)
              .add("--module-path", modulePath)
              .add("--module-source-path", src + "/*/" + realm + "/java")
              .add("--module-version", version)
              .add("--module", String.join(",", modules)));

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
      MODULES("*", "List of modules to build. '*' means all in PATH_SRC."),
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

    public static Project of(Path home, Path work) {
      return new Project(home, work);
    }

    final Properties properties;
    final String name;
    final String version;

    final Path bin;
    final Path lib;
    final Path src;

    private Project(Path home, Path work) {
      this.properties = Run.newProperties(home);
      this.name = Property.NAME.get(properties, "" + home.toAbsolutePath().normalize().getFileName());
      this.version = Property.VERSION.get(properties);

      this.bin = work.resolve(path(Property.PATH_BIN));
      this.lib = home.resolve(path(Property.PATH_LIB));
      this.src = home.resolve(path(Property.PATH_SRC));
    }

    String get(Property property) {
      return property.get(properties);
    }

    List<String> modules(String realm) {
      return modules(realm, get(Property.MODULES), src);
    }

    static List<String> modules(String realm, String userDefinedModules, Path sourceDirectory) {
      if ("*".equals(userDefinedModules)) {
        // Find modules for "src/.../*/${realm}/java"
        if (Files.notExists(sourceDirectory)) {
          return List.of();
        }
        var modules = new ArrayList<String>();
        var descriptor = Path.of(realm, "java", "module-info.java");
        DirectoryStream.Filter<Path> filter =
            path -> Files.isDirectory(path) && Files.exists(path.resolve(descriptor));
        try (var stream = Files.newDirectoryStream(sourceDirectory, filter)) {
          stream.forEach(directory -> modules.add(directory.getFileName().toString()));
        } catch (Exception e) {
          throw new Error("Scanning directory for modules failed: " + e);
        }
        return modules;
      }
      var modules = userDefinedModules.split(",");
      for (int i = 0; i < modules.length; i++) {
        modules[i] = modules[i].strip();
      }
      return List.of(modules);
    }

    List<Path> modulePath(String realm, String phase, String... requiredRealms) {
      var result = new ArrayList<Path>();
      if ("runtime".equals(phase)) {
        result.add(bin.resolve(realm).resolve("modules"));
      }
      var candidates = List.of(realm, realm + "-" + phase + "-junit", realm + "-" + phase + "-only");
      for (var candidate : candidates) {
        result.add(lib.resolve(candidate));
      }
      for (var required : requiredRealms) {
        if (realm.equals(required)) {
          throw new IllegalArgumentException("Cyclic realm dependency detected: " + realm);
        }
        bin.resolve(required).resolve("modules");
        result.addAll(modulePath(required, phase));
      }
      result.removeIf(Files::notExists);
      return result;
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

  /** Bach consuming no-arg task operating via side-effects. */
  @FunctionalInterface
  public interface Task {

    /** Performs this task on the given Bach instance. */
    void perform(Bach bach) throws Exception;

    /** Transform a name and arguments into a task object. */
    static Task of(String name, Deque<String> arguments) {
      // try {
      //   var method = Bach.class.getMethod(name);
      //   return bach -> method.invoke(bach);
      // } catch (ReflectiveOperationException e) {
      //   // fall-through
      // }
      try {
        var taskClass = Class.forName(name);
        if (Task.class.isAssignableFrom(taskClass)) {
          return (Task) taskClass.getConstructor().newInstance();
        }
        throw new IllegalArgumentException(taskClass + " doesn't implement " + Task.class);
      } catch (ReflectiveOperationException e) {
        // fall-through
      }
      var defaultTask = Task.Default.valueOf(name.toUpperCase());
      return defaultTask.consume(arguments);
    }

    /** Transform strings to tasks. */
    static List<Task> of(List<String> args) {
      var tasks = new ArrayList<Task>();
      if (args.isEmpty()) {
        tasks.add(Default.BUILD);
      } else {
        var arguments = new ArrayDeque<>(args);
        while (!arguments.isEmpty()) {
          var argument = arguments.removeFirst();
          tasks.add(of(argument, arguments));
        }
      }
      return tasks;
    }

    /** Default task delegating to Bach API methods. */
    enum Default implements Task {
      BUILD(Bach::build, "Build modular Java project in base directory."),
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
        /** Return new Task instance running the named tool and consuming all remaining arguments. */
        @Override
        Task consume(Deque<String> arguments) {
          var name = arguments.removeFirst();
          var args = arguments.toArray(String[]::new);
          arguments.clear();
          return bach -> bach.run.run(name, args);
        }
      };

      final Task task;
      final String[] description;

      Default(Task task, String... description) {
        this.task = task;
        this.description = description;
      }

      @Override
      public void perform(Bach bach) throws Exception {
        //        var key = "bach.task." + name().toLowerCase() + ".enabled";
        //        var enabled = Boolean.parseBoolean(bach.get(key, "true"));
        //        if (!enabled) {
        //          bach.run.info("Task " + name() + " disabled.");
        //          return;
        //        }
        task.perform(bach);
      }

      /** Return this default task instance without consuming any argument. */
      Task consume(Deque<String> arguments) {
        return this;
      }
    }
  }
}
