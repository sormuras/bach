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
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.Logger.Level.WARNING;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** Java Shell Builder. */
public class Bach {

  /** Version of Bach, {@link Runtime.Version#parse(String)}-compatible. */
  public static final String VERSION = "2-ea";

  /** Convenient short-cut to {@code "user.home"} as a path. */
  public static final Path USER_HOME = Path.of(System.getProperty("user.home"));

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

  /** Initialize this instance with text-based "log" writers and a configuration. */
  Bach(PrintWriter out, PrintWriter err, Configuration configuration) {
    this.out = Util.assigned(out, "out");
    this.err = Util.assigned(err, "err");
    this.configuration = Util.assigned(configuration, "configuration");
    this.runner = new Runner();
  }

  /** Log message unless threshold suppresses it. */
  private void log(System.Logger.Level level, String format, Object... args) {
    if (level.getSeverity() < configuration.basic.threshold.getSeverity()) {
      return;
    }
    var consumer = level.getSeverity() < WARNING.getSeverity() ? out : err;
    var message = String.format(format, args);
    consumer.println(message);
  }

  /** Main-entry point running tools indicated by the given arguments. */
  int main(List<String> arguments) {
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
  void run(int expected, String name, Object... arguments) {
    var code = runner.run(name, arguments);
    if (code != expected) {
      var message = "Tool %s(%s) returned %d, but expected %d";
      throw new AssertionError(String.format(message, name, Util.join(arguments), code, expected));
    }
  }

  /** Print Bach's version to the standard output stream. */
  public int version() {
    out.println(VERSION);
    return 0;
  }

  /** Format all Java source files of the project in-place. */
  public int format() {
    return new Formatter().format(List.of(configuration.paths.sources), true);
  }

  /** Supported property keys with default values and descriptions. */
  enum Property {
    /** Name of the project. */
    NAME("project", "Name of the project."),

    /** Version of the project. */
    VERSION("1.0.0-SNAPSHOT", "Version of the project. Must be parse-able by " + Version.class),

    /** Path to directory containing all Java module sources. */
    PATH_SOURCES("src", "Path to directory containing all Java module sources."),

    /** List of modules to compile, or '*' indicating all modules. */
    OPTIONS_MODULES("*", "List of modules to compile, or '*' indicating all modules."),

    /** Options passed to all 'javac' calls. */
    OPTIONS_JAVAC("-encoding\nUTF-8\n-parameters\n-Xlint", "Options passed to 'javac' calls."),

    /** Google Java Format Uniform Resource Identifier. */
    URI_TOOL_FORMAT(
        "https://github.com/"
            + "google/google-java-format/releases/download/google-java-format-1.7/"
            + "google-java-format-1.7-all-deps.jar",
        "Google Java Format (all-deps) JAR.");

    final String key;
    final String defaultValue;
    final String description;

    Property(String defaultValue, String description) {
      this.key = name().replace('_', '.').toLowerCase();
      this.defaultValue = defaultValue;
      this.description = description;
    }
  }

  /** Configuration support. */
  static class Configuration {

    /** Common properties. */
    static class Basic {
      /** Current logging level threshold. */
      final System.Logger.Level threshold;
      /** Custom tool map. */
      final Map<String, Tool> tools;
      /** Process builder mutator. */
      final UnaryOperator<ProcessBuilder> redirectIO;

      Basic(
          System.Logger.Level threshold,
          Map<String, Tool> tools,
          UnaryOperator<ProcessBuilder> redirectIO) {
        this.threshold = threshold;
        this.tools = Map.copyOf(tools);
        this.redirectIO = redirectIO;
      }
    }

    /** Directories, files and other paths. */
    class Paths {
      final Path home;
      final Path work;
      final Path sources;

      Paths(Path home, Path work) {
        this.home = home;
        this.work = work;
        this.sources = home.resolve(get(Property.PATH_SOURCES));
      }
    }

    /** Default options passed to various tools. */
    class Options {

      final List<String> modules = modules(get(Property.OPTIONS_MODULES));
      final List<String> javac = lines(Property.OPTIONS_JAVAC);

      private List<String> modules(String modules) {
        if ("*".equals(modules)) {
          return Util.findDirectoryNames(paths.sources);
        }
        return List.of(modules.split("\\s*,\\s*"));
      }
    }

    /** Default uris. */
    class Uris {

      final URI toolFormat = uri(Property.URI_TOOL_FORMAT);
    }

    /** Create new properties potentially loading contents from the given path. */
    private static Properties properties(Path path) {
      if (Files.isRegularFile(path)) {
        return Util.loadProperties(path);
      }
      assert Files.isDirectory(path) : "Expected a directory, but got: " + path;
      var directory = Objects.toString(path.getFileName(), Property.NAME.defaultValue);
      for (var name : List.of(directory, "bach", "")) {
        var file = path.resolve(name + ".properties");
        if (Files.isRegularFile(file)) {
          return Util.loadProperties(file);
        }
      }
      return new Properties();
    }

    /** Create configuration based on the given path, either a directory or a properties file. */
    public static Configuration of(Path path) {
      var debug = System.getProperty("debug".substring(1)) != null;
      var level = debug ? DEBUG : INFO;
      var basic = new Basic(level, Map.of(), ProcessBuilder::inheritIO);

      var parent = Optional.ofNullable(path.getParent()).orElse(Path.of(""));
      var home = Files.isDirectory(path) ? path : parent;
      return new Configuration(basic, home, home, properties(path));
    }

    /** Create configuration using given basics and scanning home directory for properties. */
    static Configuration of(Basic basic, Path home, Path work) {
      return new Configuration(basic, home, work, properties(home));
    }

    final Map<Property, Object> map;
    final Properties properties;
    final Basic basic;
    final Paths paths;
    final Options options;
    final Uris uris;
    final Project project;

    private Configuration(Basic basic, Path home, Path work, Properties properties) {
      this.map = new EnumMap<>(Property.class);
      this.basic = basic;
      this.properties = properties;
      this.paths = new Paths(home, work);
      this.options = new Options();
      this.uris = new Uris();
      this.project = new Project(this);
    }

    String actual(Property property) {
      var value = Objects.toString(map.getOrDefault(property, property.defaultValue));
      return value + actualUnrolled(property);
    }

    String actualUnrolled(Property property) {
      switch (property) {
        case OPTIONS_MODULES:
          return " -> " + String.join(",", options.modules);
      }
      return "";
    }

    String get(Property property) {
      return get(property, () -> property.defaultValue);
    }

    String get(Property property, Supplier<Object> defaultValueSupplier) {
      var value = Util.get(property.key, properties, defaultValueSupplier);
      map.putIfAbsent(property, value);
      return value;
    }

    List<String> lines(Property property) {
      return get(property).lines().collect(Collectors.toList());
    }

    URI uri(Property property) {
      return URI.create(get(property));
    }
  }

  /** Modular project model. */
  static class Project {
    final String name;
    final Version version;

    Project(Configuration configuration) {
      var directory = configuration.paths.home.toAbsolutePath();
      this.name = configuration.get(Property.NAME, directory::getFileName);
      this.version = Version.parse(configuration.get(Property.VERSION));
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
      log(INFO, ">> %s(%s)", name, Util.join(arguments));

      var configuredTool = configuration.basic.tools.get(name);
      if (configuredTool != null) {
        log(DEBUG, "Running configured tool named '%s'...", configuredTool.name());
        return configuredTool.run(Bach.this);
      }

      var providedTool = ToolProvider.findFirst(name);
      if (providedTool.isPresent()) {
        var tool = providedTool.get();
        log(DEBUG, "Running provided tool: %s", tool);
        return tool.run(out, err, Util.strings(arguments));
      }

      var apiTool = Tool.API.get(name);
      if (apiTool != null) {
        log(DEBUG, "Running API tool named %s", apiTool.name());
        return apiTool.run(Bach.this);
      }

      var javaBinaries = Path.of(System.getProperty("java.home")).resolve("bin");
      var javaExecutable = Util.findExecutable(List.of(javaBinaries), name);
      if (javaExecutable.isPresent()) {
        var processBuilder = new ProcessBuilder(javaExecutable.get().toString());
        processBuilder.command().addAll(List.of(Util.strings(arguments)));
        processBuilder.environment().put("BACH_VERSION", Bach.VERSION);
        processBuilder.environment().put("BACH_HOME", configuration.paths.home.toString());
        processBuilder.environment().put("BACH_WORK", configuration.paths.work.toString());
        log(DEBUG, "Starting new process: %s", processBuilder);
        return run(configuration.basic.redirectIO.apply(processBuilder));
      }

      log(ERROR, "Unknown tool '%s', returning non-zero error code", name);
      return 42;
    }

    /** Start new process and wait for its termination. */
    int run(ProcessBuilder processBuilder) {
      try {
        var process = processBuilder.start();
        var code = process.waitFor();
        if (code == 0) {
          log(DEBUG, "Process '%s' successfully terminated.", process);
        }
        return code;
      } catch (Exception e) {
        throw new Error("Starting process failed: " + e);
      }
    }
  }

  /** Download helper. */
  class Downloader {
    final Path destination;

    Downloader(Path destination) {
      this.destination = destination;
    }

    /** Download an artifact from a Maven 2 repository specified by its GAV coordinates. */
    Path download(String group, String artifact, String version) {
      log(TRACE, "Downloader::download(%s, %s, %s)", group, artifact, version);
      var host = "https://repo1.maven.org/maven2";
      var path = group.replace('.', '/');
      var file = artifact + '-' + version + ".jar";
      var uri = URI.create(String.join("/", host, path, artifact, version, file));
      return download(uri, Boolean.getBoolean("bach.offline"));
    }

    /** Download a file denoted by the specified uri. */
    Path download(URI uri, boolean offline) {
      log(TRACE, "Downloader::download(%s)", uri);
      try {
        var fileName = extractFileName(uri);
        var target = Files.createDirectories(destination).resolve(fileName);
        var url = uri.toURL(); // fails for non-absolute uri
        if (offline) {
          log(DEBUG, "Offline mode is active!");
          if (Files.exists(target)) {
            var file = target.getFileName().toString();
            log(DEBUG, "Target already exists: %s, %d bytes.", file, Files.size(target));
            return target;
          }
          var message = "Offline mode is active and target is missing: " + target;
          log(ERROR, message);
          throw new IllegalStateException(message);
        }
        return download(uri, url.openConnection());
      } catch (IOException e) {
        throw new UncheckedIOException("Download failed!", e);
      }
    }

    /** Download a file using the given URL connection. */
    Path download(URI uri, URLConnection connection) throws IOException {
      var millis = connection.getLastModified(); // 0 means "unknown"
      var lastModified = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
      log(TRACE, "Remote was modified on %s", lastModified);
      var target = destination.resolve(extractFileName(connection));
      log(TRACE, "Local target file is %s", target.toUri());
      var file = target.getFileName().toString();
      if (Files.exists(target)) {
        var fileModified = Files.getLastModifiedTime(target);
        log(TRACE, "Local last modified on %s", fileModified);
        if (fileModified.equals(lastModified)) {
          log(TRACE, "Timestamp match: %s, %d bytes.", file, Files.size(target));
          connection.getInputStream().close(); // release all opened resources
          return target;
        }
        log(DEBUG, "Local target file differs from remote source -- replacing it...");
      }
      log(INFO, ">> download(%s)", uri);
      try (var sourceStream = connection.getInputStream()) {
        try (var targetStream = Files.newOutputStream(target)) {
          sourceStream.transferTo(targetStream);
        }
        Files.setLastModifiedTime(target, lastModified);
      }
      log(DEBUG, "Downloaded %s [%d bytes from %s]", file, Files.size(target), lastModified);
      return target;
    }

    /** Extract last path element from the supplied uri. */
    String extractFileName(URI uri) {
      var path = uri.getPath(); // strip query and fragment elements
      return path.substring(path.lastIndexOf('/') + 1);
    }

    /** Extract target file name either from 'Content-Disposition' header or. */
    String extractFileName(URLConnection connection) {
      var contentDisposition = connection.getHeaderField("Content-Disposition");
      if (contentDisposition != null && contentDisposition.indexOf('=') > 0) {
        return contentDisposition.split("=")[1].replaceAll("\"", "");
      }
      try {
        return extractFileName(connection.getURL().toURI());
      } catch (URISyntaxException e) {
        throw new Error("URL connection returned invalid URL?!", e);
      }
    }
  }

  /** Format Java source files. */
  class Formatter {

    /** Run format. */
    int format(Object... args) {
      log(TRACE, "format(%s)", Util.join(args));
      var uri = configuration.uris.toolFormat;
      var downloader = new Downloader(USER_HOME.resolve(".bach/tool/format"));
      var jar = downloader.download(uri, Boolean.getBoolean("bach.offline"));
      var arguments = new ArrayList<>();
      arguments.add("-jar");
      arguments.add(jar);
      arguments.addAll(List.of(args));
      return runner.run("java", arguments.toArray(Object[]::new));
    }

    /** Run format. */
    int format(Iterable<Path> roots, boolean replace) {
      var files = Util.find(roots, Util::isJavaFile);
      if (files.isEmpty()) {
        return 0;
      }
      var args = new ArrayList<>();
      args.addAll(replace ? List.of("--replace") : List.of("--dry-run", "--set-exit-if-changed"));
      args.addAll(files);
      return format(args.toArray(Object[]::new));
    }
  }

  /** Custom tool interface. */
  @FunctionalInterface
  public interface Tool {

    /** Default tools. */
    Map<String, Tool> API = Map.of("format", Bach::format, "version", Bach::version);

    default String name() {
      return getClass().getName();
    }

    int run(Bach bach);
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
    /** List all paths matching the given filter starting at given root paths. */
    static List<Path> find(Iterable<Path> roots, Predicate<Path> filter) {
      var files = new ArrayList<Path>();
      for (var root : roots) {
        try (var stream = Files.walk(root)) {
          stream.filter(filter).forEach(files::add);
        } catch (Exception e) {
          throw new Error("Scanning directory '" + root + "' failed: " + e, e);
        }
      }
      return files;
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

    /** Test supplied path for pointing to a Java source compilation unit. */
    static boolean isJarFile(Path path) {
      return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar");
    }

    /** Test supplied path for pointing to a Java module declaration source compilation unit. */
    static boolean isModuleInfo(Path path) {
      return Files.isRegularFile(path) && path.getFileName().toString().equals("module-info.java");
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
    static Optional<Path> findExecutable(Iterable<Path> paths, String name) {
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
