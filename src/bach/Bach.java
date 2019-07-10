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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.DirectoryNotEmptyException;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
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
  /** Modular project model instance. */
  final Project project;

  /** Initialize this instance with text-based "log" writers and a configuration. */
  Bach(PrintWriter out, PrintWriter err, Configuration configuration) {
    this.out = Util.assigned(out, "out");
    this.err = Util.assigned(err, "err");
    this.configuration = Util.assigned(configuration, "configuration");
    this.runner = new Runner();
    this.project = new Project();
  }

  /** Log message unless threshold suppresses it. */
  private void log(System.Logger.Level level, String format, Object... args) {
    if (level.getSeverity() < configuration.basic.threshold().getSeverity()) {
      return;
    }
    var consumer = level.getSeverity() < WARNING.getSeverity() ? out : err;
    var message = String.format(format, args);
    consumer.println(message);
  }

  /** Main-entry point running tools indicated by the given arguments. */
  int main(List<String> arguments) {
    log(INFO, "Bach.java %s building %s", VERSION, project);
    log(DEBUG, "  arguments=%s", Util.assigned(arguments, "arguments"));
    log(DEBUG, "Configuration");
    log(DEBUG, "  home='%s'", configuration.home);
    log(DEBUG, "  work='%s'", configuration.work);
    log(DEBUG, "  javac=%s", String.join(", ", configuration.lines(Property.OPTIONS_JAVAC)));
    log(DEBUG, "Tools");
    log(DEBUG, "  api=%s", Util.sorted(Tool.API));
    log(DEBUG, "  basic=%s", Util.sorted(configuration.basic.tools()));
    log(DEBUG, "  provided=%s", Tool.PROVIDED);
    log(DEBUG, "Project");
    log(DEBUG, "  name=%s", project.name);
    log(DEBUG, "  version=%s", project.version);
    log(DEBUG, "  modules=%s", project.modules);
    project.main.debug();
    project.test.debug();

    if (arguments.isEmpty()) {
      return build();
    }

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

  /** Build the modular project. */
  public int build() {
    log(TRACE, "Bach::build");
    if (sync() != 0) {
      log(ERROR, "Synchronization failed.");
      return 1;
    }
    if (compile() != 0) {
      log(ERROR, "Compilation failed.");
      return 1;
    }
    if (summary() != 0) {
      log(ERROR, "Summary generation failed.");
      return 1;
    }
    log(DEBUG, "Build successful.");
    return 0;
  }

  /** Print Bach's version to the standard output stream. */
  public int version() {
    log(TRACE, "Bach::version");
    out.println(VERSION);
    return 0;
  }

  /** Format all Java source files of the project in-place. */
  public int format() {
    log(TRACE, "Bach::format");
    return new Formatter().format(List.of(project.sources), true);
  }

  /** Resolve required external assets, like 3rd-party modules. */
  public int sync() {
    log(TRACE, "Bach::sync");
    if (configuration.basic.isOffline()) {
      log(DEBUG, "Offline mode is active, no synchronization.");
      return 0;
    }
    new Synchronizer().sync(project.library, "module-uri.properties");
    // TODO syncMissingLibrariesByParsingModuleDescriptors();
    return 0;
  }

  /** Print usage help. */
  public int help() {
    log(TRACE, "Bach::help");
    out.println("Usage: Bach.java <options>");
    out.println("");
    out.println("Properties, passed via '-D' or a .properties file");
    for (var property : Property.values()) {
      out.println(property.key);
      out.println("  " + property.description);
      out.println("  Configured to: " + configuration.get(property).replace('\n', ' '));
      out.println("  Default value: " + property.defaultValue.replace('\n', ' '));
    }
    return 0;
  }

  /** Compile all modules. */
  public int compile() {
    log(TRACE, "Bach::compile");
    return new Compiler().compile();
  }

  /** Print build summary. */
  public int summary() {
    log(TRACE, "Bach::summary");
    out.println(project);
    out.println(project.modules);
    return 0;
  }

  /** Supported property keys with default values and descriptions. */
  enum Property {
    /** Name of the project. */
    NAME("project", "Name of the project.") {
      @Override
      String defaultValue(Configuration configuration) {
        var name = configuration.home.normalize().toAbsolutePath().getFileName();
        return name != null ? name.toString() : defaultValue;
      }
    },

    /** Version of the project. */
    VERSION("1.0.0-SNAPSHOT", "Version of the project. Must be parse-able by " + Version.class),

    /** List of modules to compile, or '*' indicating all modules. */
    MODULES("*", "List of modules to compile, or '*' indicating all modules."),

    /** Path to directory containing all Java module sources. */
    PATH_SOURCES("src", "Path to directory containing all Java module sources."),

    /** Path to directory containing all required Java modules. */
    PATH_LIBRARY("lib", "Path to directory containing all required Java modules."),

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

    String defaultValue(Configuration configuration) {
      return defaultValue;
    }

    /** Gets a property value indicated by the specified {@code key}. */
    String get(Configuration configuration, Properties properties) {
      return Optional.ofNullable(System.getProperty(Util.assigned(key, "key")))
          .or(() -> Optional.ofNullable(Util.assigned(properties, "properties").getProperty(key)))
          .or(() -> Optional.ofNullable(System.getenv("BACH_" + key.toUpperCase())))
          .orElse(defaultValue(configuration));
    }
  }

  /** Configuration. */
  static class Configuration {

    /** Basic setup fixtures. */
    static class Basic {
      boolean isOffline() {
        return System.getProperty("offline") != null;
      }

      /** Logger level threshold. */
      System.Logger.Level threshold() {
        var debug = System.getProperty("debug".substring(1)) != null;
        return debug ? DEBUG : INFO;
      }

      /** Custom tool map. */
      Map<String, Tool> tools() {
        return Map.of();
      }

      /** ProcessBuilder mutator. */
      UnaryOperator<ProcessBuilder> redirectIO() {
        return ProcessBuilder::inheritIO;
      }
    }

    /** Create new properties instance potentially loading contents from the given path. */
    static Properties properties(Path path) {
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
      var parent = Optional.ofNullable(path.getParent()).orElse(Path.of(""));
      var home = Files.isDirectory(path) ? path : parent;
      return new Configuration(new Basic(), home, home, properties(path));
    }

    /** Create configuration using given basics and scanning home directory for properties. */
    static Configuration of(Basic basic, Path home, Path work) {
      return new Configuration(basic, home, work, properties(home));
    }

    final Basic basic;
    final Path home;
    final Path work;
    final Map<Property, String> map;

    Configuration(Basic basic, Path home, Path work, Properties properties) {
      this.basic = basic;
      this.home = home;
      this.work = work;
      this.map = new EnumMap<>(Property.class);
      for (var property : Property.values()) {
        map.put(property, property.get(this, properties));
      }
    }

    String get(Property property) {
      return map.get(property);
    }

    List<String> lines(Property property) {
      return get(property).lines().collect(Collectors.toList());
    }

    Path path(Property property) {
      return Path.of(get(property));
    }

    URI uri(Property property) {
      return URI.create(get(property));
    }
  }

  /** Modular project model. */
  class Project {
    final String name;
    final Version version;
    final Path sources, library;
    final List<String> modules;

    final MainRealm main;
    final TestRealm test;

    Project() {
      this.name = configuration.get(Property.NAME);
      this.version = Version.parse(configuration.get(Property.VERSION));
      this.sources = configuration.home.resolve(configuration.path(Property.PATH_SOURCES));
      this.library = configuration.home.resolve(configuration.path(Property.PATH_LIBRARY));
      this.modules = modules();
      this.main = new MainRealm();
      this.test = new TestRealm(main);
    }

    private List<String> modules() {
      var modules = configuration.get(Property.MODULES);
      if ("*".equals(modules)) {
        return Util.findDirectoryNames(sources);
      }
      return List.of(modules.split("\\s*,\\s*"));
    }

    @Override
    public String toString() {
      return name + ' ' + version;
    }

    abstract class Realm {
      final String name;
      final String moduleSourcePath;
      final Map<String, ModuleDescriptor> declaredModules;
      final Set<String> externalModules;

      Realm(String name) {
        this.name = name;

        var moduleSourcePaths = new TreeSet<String>();
        var descriptors = new TreeMap<String, ModuleDescriptor>();
        var declarations = Util.find(sources, Util::isModuleInfo);
        for (var declaration : declarations) {
          //  <module>/<realm>/.../module-info.java
          var relative = sources.relativize(declaration);
          var module = relative.getName(0).toString();
          var realm = relative.getName(1).toString();
          if (!modules.contains(module)) {
            continue; // module not selected in this project's configuration
          }
          if (!name.equals(realm)) {
            continue; // not our realm
          }
          var descriptor = Modules.parseDeclaration(declaration);
          assert module.equals(descriptor.name()) : module + " expected, but got: " + descriptor;
          descriptors.put(module, descriptor);
          var offset = relative.subpath(1, relative.getNameCount() - 1).toString();
          moduleSourcePaths.add(String.join(File.separator, sources.toString(), "*", offset));
        }
        this.moduleSourcePath = String.join(File.pathSeparator, moduleSourcePaths);
        this.declaredModules = Collections.unmodifiableMap(descriptors);
        this.externalModules = Modules.findExternalModuleNames(descriptors.values());
      }

      void debug() {
        log(DEBUG, "Realm: %s", name);
        log(DEBUG, "  moduleSourcePath=%s", moduleSourcePath);
        log(DEBUG, "  declaredModules=%s", declaredModules.keySet());
        log(DEBUG, "  externalModules=%s", externalModules);
      }
    }

    class MainRealm extends Realm {

      MainRealm() {
        super("main");
      }
    }

    class TestRealm extends Realm {

      final MainRealm main;

      TestRealm(MainRealm main) {
        super("test");
        this.main = main;
      }
    }
  }

  /** Tool-invoking dispatcher. */
  class Runner {

    /** Run named tool with specified arguments returning an error code. */
    int run(String name, Object... arguments) {
      log(INFO, ">> %s(%s)", name, Util.join(arguments));

      var configuredTool = configuration.basic.tools().get(name);
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
        processBuilder.environment().put("BACH_HOME", configuration.home.toString());
        processBuilder.environment().put("BACH_WORK", configuration.work.toString());
        log(DEBUG, "Starting new process: %s", processBuilder);
        return run(configuration.basic.redirectIO().apply(processBuilder));
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
      return download(uri);
    }

    /** Download a file denoted by the specified uri. */
    Path download(URI uri) {
      log(TRACE, "Downloader::download(%s)", uri);
      try {
        var fileName = extractFileName(uri);
        var target = Files.createDirectories(destination).resolve(fileName);
        var url = uri.toURL(); // fails for non-absolute uri
        if (configuration.basic.isOffline()) {
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
      var uri = configuration.uri(Property.URI_TOOL_FORMAT);
      var downloader = new Downloader(USER_HOME.resolve(".bach/tool/format"));
      var jar = downloader.download(uri);
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

  /** Resolve external modules. */
  class Synchronizer {

    void sync(Path root, String fileName) {
      log(DEBUG, "Synchronizing 3rd-party module uris below: %s", root.toUri());
      if (Files.notExists(root)) {
        log(DEBUG, "Not synchronizing because directory doesn't exist: %s", root);
        return;
      }
      var paths = Util.find(Set.of(root), path -> path.getFileName().toString().equals(fileName));
      var synced = new ArrayList<Path>();
      for (var path : paths) {
        var directory = path.getParent();
        var downloader = new Downloader(directory);
        var properties = Util.loadProperties(path);
        if (properties.isEmpty()) {
          log(DEBUG, "No module uri declared in %s", path.toUri());
          continue;
        }
        log(DEBUG, "Syncing %d module uri(s) to %s", properties.size(), directory.toUri());
        for (var value : properties.values()) {
          var string = value.toString();
          var uri = URI.create(string);
          uri = uri.isAbsolute() ? uri : configuration.home.resolve(string).toUri();
          log(DEBUG, "Syncing %s", uri);
          var target = downloader.download(uri);
          synced.add(target);
          log(DEBUG, " o %s", target.toUri());
        }
      }
      log(DEBUG, "Synchronized %d module uri(s).", synced.size());
    }
  }

  /** Compiles all modules of the project. */
  class Compiler {

    /** Multi-release module compiler. */
    class Hydra {
      List<String> compile(Project.Realm realm, List<String> modules) {
        return List.of();
      }
    }

    /** Default multi-module compiler. */
    class Jigsaw {
      List<String> compile(Project.Realm realm, List<String> modules) {
        return modules;
      }
    }

    int compile() {
      if (compile(project.main) != 0) return 1;
      if (compile(project.test) != 0) return 1;
      return 0;
    }

    private int compile(Project.Realm realm) {
      var modules = new ArrayList<>(realm.declaredModules.keySet());
      if (modules.isEmpty()) {
        log(INFO, "No %s modules declared -- skip compilation.", realm.name);
        return 0;
      }
      log(DEBUG, "Compiling %d %s module(s): %s", modules.size(), realm.name, modules);
      modules.removeAll(new Hydra().compile(realm, modules));
      modules.removeAll(new Jigsaw().compile(realm, modules));
      if (modules.isEmpty()) {
        return 0;
      }
      log(ERROR, "Not compiled module(s): " + modules);
      return 1;
    }
  }

  /** Custom tool interface. */
  @FunctionalInterface
  public interface Tool {

    /** Default tools. */
    Map<String, Tool> API =
        Map.of(
            "build", Bach::build,
            "compile", Bach::compile,
            "format", Bach::format,
            "help", Bach::help,
            "summary", Bach::summary,
            "sync", Bach::sync,
            "version", Bach::version);

    /** Tools provided by the Java runtime. */
    List<String> PROVIDED =
        ServiceLoader.load(ToolProvider.class).stream()
            .map(ServiceLoader.Provider::get)
            .map(ToolProvider::name)
            .sorted()
            .collect(Collectors.toList());

    default String name() {
      return getClass().getName();
    }

    int run(Bach bach);
  }

  /** Static helpers handling modules. */
  static class Modules {

    private static final Pattern MODULE_NAME_PATTERN = Pattern.compile("(?:module)\\s+(.+)\\s*\\{");
    private static final Pattern MODULE_REQUIRES_PATTERN =
        Pattern.compile(
            "(?:requires)" // key word
                + "(?:\\s+[\\w.]+)?" // optional modifiers
                + "\\s+([\\w.]+)" // module name
                + "(?:\\s*/\\*\\s*([\\w.\\-+]+)\\s*\\*/\\s*)?" // optional '/*' version '*/'
                + ";"); // end marker

    /** Enumerate all system module names. */
    static List<String> findSystemModuleNames() {
      return ModuleFinder.ofSystem().findAll().stream()
          .map(reference -> reference.descriptor().name())
          .sorted()
          .collect(Collectors.toList());
    }

    /** Calculate external module names. */
    static Set<String> findExternalModuleNames(Iterable<ModuleDescriptor> descriptors) {
      var declaredModules = new TreeSet<String>();
      var requiredModules = new TreeSet<String>();
      for (var descriptor : descriptors) {
        declaredModules.add(descriptor.name());
        descriptor.requires().stream().map(Requires::name).forEach(requiredModules::add);
      }
      var externalModules = new TreeSet<>(requiredModules);
      externalModules.removeAll(declaredModules);
      externalModules.removeAll(findSystemModuleNames()); // "java.base", "java.logging", ...
      return Set.copyOf(externalModules);
    }

    /** Simplistic module declaration parser. */
    static ModuleDescriptor parseDeclaration(Path path) {
      if (!Util.isModuleInfo(path)) {
        throw new IllegalArgumentException("Expected module-info.java path, but got: " + path);
      }
      try {
        return parseDeclaration(Files.readString(path));
      } catch (IOException e) {
        throw new UncheckedIOException("Reading module declaration failed: " + path, e);
      }
    }

    /** Simplistic module declaration parser. */
    static ModuleDescriptor parseDeclaration(String source) {
      var nameMatcher = MODULE_NAME_PATTERN.matcher(source);
      if (!nameMatcher.find()) {
        throw new IllegalArgumentException("Expected Java module source unit, but got: " + source);
      }
      var name = nameMatcher.group(1).trim();
      var builder = ModuleDescriptor.newModule(name);
      var requiresMatcher = MODULE_REQUIRES_PATTERN.matcher(source);
      while (requiresMatcher.find()) {
        var requiredName = requiresMatcher.group(1);
        Optional.ofNullable(requiresMatcher.group(2))
            .ifPresentOrElse(
                version -> builder.requires(Set.of(), requiredName, Version.parse(version)),
                () -> builder.requires(requiredName));
      }
      return builder.build();
    }
  }

  /** Static helper. */
  static class Util {

    private Util() {
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

    /** Convert keys of the given map to a sorted list of each keys' string representation. */
    static List<String> sorted(Map<?, ?> map) {
      return map.keySet().stream().map(Object::toString).sorted().collect(Collectors.toList());
    }

    /** Convert given array of objects to an array of strings. */
    static String[] strings(Object... objects) {
      var list = new ArrayList<String>();
      for (var object : objects) {
        list.add(Objects.toString(object, null));
      }
      return list.toArray(String[]::new);
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
