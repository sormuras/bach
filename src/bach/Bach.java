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
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.Logger.Level.WARNING;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import java.util.function.Consumer;
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
    log(DEBUG, "Tools");
    log(DEBUG, "  api=%s", Util.sorted(Tool.API));
    log(DEBUG, "  basic=%s", Util.sorted(configuration.basic.tools()));
    log(DEBUG, "  provided=%s", Tool.PROVIDED);
    log(DEBUG, "Options");
    log(DEBUG, "  javac=%s", String.join(", ", configuration.lines(Property.OPTIONS_JAVAC)));
    log(DEBUG, "  junit=%s", String.join(", ", configuration.lines(Property.OPTIONS_JUNIT)));
    log(DEBUG, "Project");
    log(DEBUG, "  name=%s", project.name);
    log(DEBUG, "  version=%s", project.version);
    log(DEBUG, "  sources='%s'", project.sources);
    log(DEBUG, "  modules=%s", project.modules);
    log(DEBUG, "  library='%s'", project.library);
    log(DEBUG, "  requires=%s", project.requires);
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
    if (test() != 0) {
      log(ERROR, "Test run failed.");
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

  /** Test all modules. */
  public int test() {
    log(TRACE, "Bach::test");
    return new Tester().test(project.modules);
  }

  /** Print build summary. */
  public int summary() {
    log(TRACE, "Bach::summary");
    var path = project.main.binModules;
    if (Files.notExists(path)) {
      out.println("No module destination directory created: " + path.toUri());
      return 0;
    }
    var jars = Util.find(List.of(path), Util::isJarFile);
    if (jars.isEmpty()) {
      out.printf("No module created for %s%n", project.name);
      return 0;
    }
    if (configuration.verbose()) {
      for (var jar : jars) {
        runner.run(new Command("jar", "--describe-module", "--file", jar));
      }
      runner.run(
          new Command(
              "jdeps",
              "--multi-release",
              "base",
              "--module-path",
              project.main.binModules,
              "--check",
              String.join(",", project.main.declaredModules.keySet())));
    }
    out.printf("%d main module(s) created in %s%n", jars.size(), path.toUri());
    for (var jar : jars) {
      var module = ModuleFinder.of(jar).findAll().iterator().next().descriptor();
      out.printf(" -> %9d %s <- %s%n", Util.size(jar), jar.getFileName(), module);
    }
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

    /** Options passed to all 'junit' console calls. */
    OPTIONS_JUNIT("--fail-if-no-tests\n--disable-banner\n--details=tree", "'junit' run options."),

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
      var normalized = path.toAbsolutePath().normalize();
      var directory = Objects.toString(normalized.getFileName(), Property.NAME.defaultValue);
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

    boolean verbose() {
      return basic.threshold().getSeverity() <= DEBUG.getSeverity();
    }
  }

  /** Command-line program argument list builder. */
  static class Command {

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

    /** Add two (or zero) arguments, the key and the paths joined by system's path separator. */
    Command add(Object key, Collection<Path> paths) {
      return add(key, paths, UnaryOperator.identity());
    }

    /** Add two (or zero) arguments, the key and the paths joined by system's path separator. */
    Command add(Object key, Collection<Path> paths, UnaryOperator<String> operator) {
      var stream = paths.stream().filter(Files::exists).map(Object::toString);
      var value = stream.collect(Collectors.joining(File.pathSeparator));
      if (value.isEmpty()) {
        return this;
      }
      return add(key, operator.apply(value));
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

    /** Add two arguments iff the conditions is {@code true}. */
    Command addIff(boolean condition, Object key, Object value) {
      return condition ? add(key, value) : this;
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

  /** Modular project model. */
  class Project {
    final String name;
    final Version version;
    final Path sources, library;
    final List<String> modules;
    final Set<String> requires;

    final MainRealm main;
    final TestRealm test;

    Project() {
      this.name = configuration.get(Property.NAME);
      this.version = Version.parse(configuration.get(Property.VERSION));
      this.sources = configuration.home.resolve(configuration.path(Property.PATH_SOURCES));
      this.library = configuration.home.resolve(configuration.path(Property.PATH_LIBRARY));
      this.modules = modules();
      var references = Modules.findModuleInfoReferences(sources);
      this.requires = requires(references);
      this.main = new MainRealm(references);
      this.test = new TestRealm(references, main);
    }

    private List<String> modules() {
      var modules = configuration.get(Property.MODULES);
      if ("*".equals(modules)) {
        return Util.findDirectoryNames(sources);
      }
      return Arrays.stream(modules.split(",")).map(String::strip).collect(Collectors.toList());
    }

    private Set<String> requires(List<Modules.ModuleInfoReference> references) {
      var descriptors = references.stream().map(ModuleReference::descriptor);
      return Modules.findExternalModuleNames(descriptors.collect(Collectors.toSet()));
    }

    @Override
    public String toString() {
      return name + ' ' + version;
    }

    abstract class Realm {
      final String name;
      final String moduleSourcePath;
      final Map<String, ModuleDescriptor> declaredModules;
      final Path binaries;
      final Path binModules;
      final Path binSources;

      Realm(List<Modules.ModuleInfoReference> references, String name) {
        this.name = name;

        var bin = configuration.work.resolve("bin");
        this.binaries = bin.resolve(name);
        this.binModules = binaries.resolve("modules");
        this.binSources = binaries.resolve("sources");

        var moduleSourcePaths = new TreeSet<String>();
        var descriptors = new TreeMap<String, ModuleDescriptor>();
        for (var moduleInfo : references) {
          //  <module>/<realm>/.../module-info.java
          var relative = sources.relativize(moduleInfo.path());
          var module = relative.getName(0).toString();
          var realm = relative.getName(1).toString();
          if (!modules.contains(module)) {
            continue; // module not selected in this project's configuration
          }
          if (!name.equals(realm)) {
            continue; // not our realm
          }
          var descriptor = moduleInfo.descriptor();
          assert module.equals(descriptor.name()) : module + " expected, but got: " + descriptor;
          descriptors.put(module, descriptor);
          var offset = relative.subpath(1, relative.getNameCount() - 1).toString();
          moduleSourcePaths.add(String.join(File.separator, sources.toString(), "*", offset));
        }
        this.moduleSourcePath = String.join(File.pathSeparator, moduleSourcePaths);
        this.declaredModules = Collections.unmodifiableMap(descriptors);
      }

      void debug() {
        log(DEBUG, "Realm: %s", name);
        log(DEBUG, "  moduleSourcePath=%s", moduleSourcePath);
        log(DEBUG, "  declaredModules=%s", declaredModules.keySet());
      }

      List<Path> modulePath(String phase) {
        var lib = configuration.home.resolve("lib");
        var paths = new ArrayList<Path>();
        paths.add(binModules); // bin/${realm}/modules
        paths.add(lib.resolve(name)); // lib/${realm} at "compile" and "test" phases
        Util.findDirectoryNames(lib).stream() // lib/${realm}-${phase}.*
            .filter(dir -> dir.startsWith(name + "-" + phase))
            .map(lib::resolve)
            .forEach(paths::add);
        return List.copyOf(paths);
      }

      void addModulePatches(Command javac, List<String> modules) {}

      Path modularJar(String module) {
        var moduleNameDashVersion = module + '-' + project.version;
        return binModules.resolve(moduleNameDashVersion + ".jar");
      }

      Path sourcesJar(String module) {
        var moduleNameDashVersion = module + '-' + project.version;
        return binSources.resolve(moduleNameDashVersion + "-sources.jar");
      }

      Set<String> packages(String module) {
        var jar = modularJar(module);
        return ModuleFinder.of(jar).find(module).orElseThrow().descriptor().packages();
      }
    }

    class MainRealm extends Realm {

      MainRealm(List<Modules.ModuleInfoReference> references) {
        super(references, "main");
      }
    }

    class TestRealm extends Realm {

      final MainRealm main;

      TestRealm(List<Modules.ModuleInfoReference> references, MainRealm main) {
        super(references, "test");
        this.main = main;
      }

      List<Path> classPathRuntime(String module) {
        var paths = new ArrayList<Path>();
        paths.add(modularJar(module));
        // Add all other test modules?
        // if (Files.isDirectory(binModules)) {
        //   paths.addAll(Util.find(Set.of(binModules), Util::isJarFile));
        // }
        if (Files.isDirectory(main.binModules)) {
          paths.addAll(Util.find(Set.of(main.binModules), Util::isJarFile));
        }
        var lib = configuration.home.resolve("lib");
        if (Files.isDirectory(lib)) {
          paths.addAll(Util.find(Set.of(lib), Util::isJarFile));
        }
        return List.copyOf(paths);
      }

      @Override
      List<Path> modulePath(String phase) {
        var paths = new ArrayList<Path>();
        paths.addAll(super.modulePath(phase)); // "test" realm
        paths.addAll(main.modulePath(phase)); // "main" realm
        return List.copyOf(paths);
      }

      List<Path> modulePathRuntime(boolean needsPatch) {
        var patchedRuntime = modulePath("runtime");
        if (needsPatch) {
          return patchedRuntime;
        }
        var paths = new ArrayList<>(patchedRuntime);
        paths.remove(test.binModules);
        paths.add(test.binModules);
        return List.copyOf(paths);
      }

      @Override
      void addModulePatches(Command javac, List<String> modules) {
        for (var module : modules) {
          if (!main.declaredModules.containsKey(module)) {
            continue;
          }
          var patch = main.moduleSourcePath.replace("*", module);
          javac.add("--patch-module", module + "=" + patch);
        }
      }
    }
  }

  /** Tool-invoking dispatcher. */
  class Runner {

    /** Run given command. */
    int run(Command command) {
      return run(command.name, command.list.toArray(Object[]::new));
    }

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
    int format(Collection<Path> roots, boolean replace) {
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

      final Pattern javaReleasePattern = Pattern.compile("java-\\d+");
      final Project.Realm realm;
      final Path destination;

      Hydra(Project.Realm realm) {
        this.realm = realm;
        this.destination = realm.binaries.resolve("compile/hydra");
      }

      private int findBaseJavaFeatureNumber(List<String> strings) {
        int base = Integer.MAX_VALUE;
        for (var string : strings) {
          var candidate = Integer.valueOf(string.substring("java-".length()));
          if (candidate < base) {
            base = candidate;
          }
        }
        if (base == Integer.MAX_VALUE) {
          throw new IllegalArgumentException("No base Java feature number found: " + strings);
        }
        return base;
      }

      List<String> compile(List<String> modules) {
        var result = new ArrayList<String>();
        for (var module : modules) {
          if (compile(module)) {
            result.add(module);
          }
        }
        return result;
      }

      private boolean compile(String module) {
        var names = Util.findDirectoryNames(project.sources.resolve(module).resolve(realm.name));
        if (names.isEmpty()) {
          return false; // empty source path or just a sole "module-info.java" file...
        }
        if (!names.stream().allMatch(javaReleasePattern.asMatchPredicate())) {
          return false;
        }
        log(DEBUG, "Building multi-release module: %s", module);
        int base = findBaseJavaFeatureNumber(names);
        log(DEBUG, "Base feature number is: %d", base);
        for (var release = base; release <= Runtime.version().feature(); release++) {
          compile(module, base, release);
        }
        try {
          jarModule(module, base);
          jarSources(module, base);
          // Create "-javadoc.jar" for this multi-release module
          // https://github.com/sormuras/make-java/issues/8
        } catch (Exception e) {
          throw new Error("Building module " + module + " failed!", e);
        }
        return true;
      }

      private void compile(String module, int base, int release) {
        var source = project.sources.resolve(module).resolve(realm.name);
        var javaR = "java-" + release;
        var sourceR = source.resolve(javaR);
        if (Files.notExists(sourceR)) {
          log(DEBUG, "Skipping %s, no source path exists: %s", javaR, sourceR);
          return;
        }
        var destinationR = destination.resolve(javaR);
        var javac =
            new Command("javac")
                .addIff(false, "-verbose")
                .addEach(configuration.lines(Property.OPTIONS_JAVAC))
                .add("--release", release);
        var compiledBase = destination.resolve("java-" + base).resolve(module);
        if (Files.notExists(sourceR.resolve("module-info.java"))) {
          var javaFiles = Util.find(List.of(sourceR), Util::isJavaFile);
          if (javaFiles.isEmpty()) {
            if (base == release) {
              throw new RuntimeException("Empty base?!");
            }
            log(DEBUG, "Skipping %s, no Java source files found in: %s", javaR, sourceR);
            return;
          }
          javac.add("-d", destinationR.resolve(module));
          var classPath = new ArrayList<Path>();
          if (release > base) {
            classPath.add(compiledBase);
          }
          if (Files.isDirectory(realm.binModules)) {
            classPath.addAll(Util.find(List.of(realm.binModules), Util::isJarFile));
          }
          // TODO classPath.addAll(Util.find(configuration.libraries.resolve(realm.name), "*.jar"));
          javac.add("--class-path", classPath);
          javac.addEach(javaFiles);
        } else {
          javac.add("-d", destinationR);
          javac.add("--module-version", project.version);
          javac.add("--module-path", realm.modulePath("compile"));
          var pathR = String.join(File.separator, "" + project.sources, "*", realm.name, javaR);
          javac.add("--module-source-path", pathR);
          javac.add("--patch-module", module + '=' + compiledBase);
          javac.add("--module", module);
        }
        if (runner.run(javac) != 0) {
          throw new RuntimeException("javac failed");
        }
      }

      private void jarModule(String module, int base) throws Exception {
        Files.createDirectories(realm.binModules);
        var javaBase = destination.resolve("java-" + base).resolve(module);
        var jar =
            new Command("jar")
                .add("--create")
                .add("--file", realm.modularJar(module))
                .addIff(configuration.verbose(), "--verbose")
                .add("-C", javaBase)
                .add(".");
        // resources
        var resources =
            configuration.home.resolve("src").resolve(realm.name + "-resources").resolve(module);
        if (Files.isDirectory(resources)) {
          jar.add("-C", resources).add(".");
        }
        // "base" + 1 .. N files
        for (var release = base + 1; release <= Runtime.version().feature(); release++) {
          var javaRelease = destination.resolve("java-" + release).resolve(module);
          if (Files.notExists(javaRelease)) {
            continue;
          }
          // store a solitary module descriptor directly in JARs root
          var fileNames = new ArrayList<String>();
          try (var stream = Files.newDirectoryStream(javaRelease, "*")) {
            stream.forEach(path -> fileNames.add(path.getFileName().toString()));
          }
          if (!(fileNames.size() == 1 && fileNames.get(0).equals("module-info.class"))) {
            jar.add("--release", release);
          }
          jar.add("-C", javaRelease);
          jar.add(".");
        }
        if (runner.run(jar) != 0) {
          throw new RuntimeException("jar failed");
        }
      }

      private void jarSources(String module, int base) throws Exception {
        var source = project.sources.resolve(module).resolve(realm.name);
        var javaBase = source.resolve("java-" + base);
        var jar =
            new Command("jar")
                .add("--create")
                .add("--file", realm.sourcesJar(module))
                .addIff(configuration.verbose(), "--verbose")
                .add("--no-manifest")
                .add("-C", javaBase)
                .add(".");
        // "base" + 1 .. N files
        for (var release = base + 1; release <= Runtime.version().feature(); release++) {
          var javaRelease = source.resolve("java-" + release);
          if (Files.notExists(javaRelease)) {
            continue;
          }
          jar.add("--release", release);
          jar.add("-C", javaRelease);
          jar.add(".");
        }
        Files.createDirectories(realm.binSources);
        if (runner.run(jar) != 0) {
          throw new RuntimeException("jar failed");
        }
      }
    }

    /** Default multi-module compiler. */
    class Jigsaw {

      List<String> compile(Project.Realm realm, List<String> modules) throws Exception {
        var destination = realm.binaries.resolve("compile/jigsaw");
        var javac =
            new Command("javac")
                .add("-d", destination)
                .addEach(configuration.lines(Property.OPTIONS_JAVAC))
                // .addIff(realm.preview, "--enable-preview")
                // .addIff(realm.release != null, "--release", realm.release)
                .add("--module-path", realm.modulePath("compile"))
                .add("--module-source-path", realm.moduleSourcePath)
                .add("--module", String.join(",", modules))
                .add("--module-version", project.version);
        realm.addModulePatches(javac, modules);
        if (runner.run(javac) != 0) {
          throw new RuntimeException("javac failed");
        }
        Files.createDirectories(realm.binModules);
        Files.createDirectories(realm.binSources);
        for (var module : modules) {
          var modularJar = realm.modularJar(module);
          var sourcesJar = realm.sourcesJar(module);
          var resources = Path.of("realm/resources"); // TODO realm.srcResources;
          var jarModule =
              new Command("jar")
                  .add("--create")
                  .add("--file", modularJar)
                  .addIff(configuration.verbose(), "--verbose")
                  .add("-C", destination.resolve(module))
                  .add(".")
                  .addIff(Files.isDirectory(resources), cmd -> cmd.add("-C", resources).add("."));
          var jarSources =
              new Command("jar")
                  .add("--create")
                  .add("--file", sourcesJar)
                  .addIff(configuration.verbose(), "--verbose")
                  .add("--no-manifest")
                  .add("-C", project.sources.resolve(module).resolve(realm.name).resolve("java"))
                  .add(".")
                  .addIff(Files.isDirectory(resources), cmd -> cmd.add("-C", resources).add("."));
          if (runner.run(jarModule) != 0) {
            throw new RuntimeException(
                "Creating " + realm.name + " modular jar failed: " + modularJar);
          }
          if (runner.run(jarSources) != 0) {
            throw new RuntimeException(
                "Creating " + realm.name + " sources jar failed: " + sourcesJar);
          }
        }
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
      try {
        modules.removeAll(new Hydra(realm).compile(modules));
        modules.removeAll(new Jigsaw().compile(realm, modules));
        if (modules.isEmpty()) {
          return 0;
        }
        log(ERROR, "Not compiled module(s): " + modules);
        return 1;
      } catch (Exception e) {
        log(ERROR, "Compilation failed:" + e);
        e.printStackTrace(err);
        return 2;
      }
    }
  }

  /** Test all modules of the project. */
  class Tester {

    int test(Collection<String> modules) {
      var errors = "";
      for (var module : modules) {
        var testModuleJar = project.test.modularJar(module);
        if (Files.notExists(testModuleJar)) {
          log(DEBUG, "No test module available for: %s", module);
          continue;
        }
        out.printf("%n%n%n%s%n%n%n", module);
        errors += testClassPathDirect(module);
        errors += testClassPathForked(module);
        errors += testModulePathDirect(module);
        errors += testModulePathForked(module);
      }
      if (errors.replace('0', ' ').isBlank()) {
        return 0;
      }
      log(ERROR, "errors=%s", errors);
      return 1;
    }

    int testClassPathDirect(String module) {
      var classPath = project.test.classPathRuntime(module);
      var urls = classPath.stream().map(Util::url).toArray(URL[]::new);
      var parentLoader = ClassLoader.getPlatformClassLoader();
      var junitLoader = new URLClassLoader("junit", urls, parentLoader);
      var junit = new Command("junit").addEach(configuration.lines(Property.OPTIONS_JUNIT));
      project.test.packages(module).forEach(path -> junit.add("--select-package", path));
      return launchJUnitPlatformConsole(junitLoader, junit);
    }

    int testClassPathForked(String module) {
      var java =
          new Command("java")
              .add("-ea")
              .add("--class-path", project.test.classPathRuntime(module))
              .add("org.junit.platform.console.ConsoleLauncher")
              .addEach(configuration.lines(Property.OPTIONS_JUNIT));
      project.test.packages(module).forEach(path -> java.add("--select-package", path));
      return runner.run(java);
    }

    int testModulePathDirect(String module) {
      var junit =
          new Command("junit")
              .addEach(configuration.lines(Property.OPTIONS_JUNIT))
              .add("--select-module", module);
      try {
        return testModulePathDirect(module, junit);
      } finally {
        var windows = System.getProperty("os.name", "?").toLowerCase().contains("win");
        if (windows) {
          System.gc();
          Util.sleep(1234);
        }
      }
    }

    int testModulePathForked(String module) {
      var needsPatch = project.main.declaredModules.containsKey(module);
      var java =
          new Command("java")
              .add("-ea")
              .add("--module-path", project.test.modulePathRuntime(needsPatch))
              .add("--add-modules", module);
      if (needsPatch) {
        java.add("--patch-module", module + "=" + project.main.modularJar(module));
      }
      java.add("--module")
          .add("org.junit.platform.console")
          .addEach(configuration.lines(Property.OPTIONS_JUNIT))
          .add("--select-module", module);
      return runner.run(java);
    }

    /** Launch JUnit Platform Console for the given module. */
    private int testModulePathDirect(String module, Command junit) {
      var needsPatch = project.main.declaredModules.containsKey(module);
      var modulePath = project.test.modulePathRuntime(needsPatch);
      log(DEBUG, "Module path:");
      for (var element : modulePath) {
        log(DEBUG, "  -> %s", element);
      }
      var finder = ModuleFinder.of(modulePath.toArray(Path[]::new));
      log(DEBUG, "Finder finds module(s):");
      for (var reference : finder.findAll()) {
        log(DEBUG, "  -> %s", reference);
      }
      var roots = List.of(module, "org.junit.platform.console");
      log(DEBUG, "Root module(s):");
      for (var root : roots) {
        log(DEBUG, "  -> %s", root);
      }
      var boot = ModuleLayer.boot();
      var configuration = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
      var parentLoader = ClassLoader.getPlatformClassLoader();
      var controller = defineModulesWithOneLoader(configuration, List.of(boot), parentLoader);
      var junitConsoleLayer = controller.layer();
      controller.addExports( // "Bach.java" resides in an unnamed module...
          junitConsoleLayer.findModule("org.junit.platform.console").orElseThrow(),
          "org.junit.platform.console",
          Bach.class.getModule());
      var junitConsoleLoader = junitConsoleLayer.findLoader("org.junit.platform.console");
      var junitLoader = new URLClassLoader("junit", new URL[0], junitConsoleLoader);
      return launchJUnitPlatformConsole(junitLoader, junit);
    }

    /** Launch JUnit Platform Console passing all arguments of the given command. */
    private int launchJUnitPlatformConsole(ClassLoader loader, Command junit) {
      log(DEBUG, "Launching JUnit Platform Console: %s", junit.list);
      log(DEBUG, "Using class loader: %s - %s", loader.getName(), loader);
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
        Bach.this.out.write(out.toString());
        Bach.this.err.write(err.toString());
        return (int) result.getClass().getMethod("getExitCode").invoke(result);
      } catch (Exception e) {
        throw new Error("ConsoleLauncher.execute(...) failed: " + e, e);
      } finally {
        currentThread.setContextClassLoader(currentContextLoader);
      }
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
            "test", Bach::test,
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

    /** Source-based module reference. */
    static class ModuleInfoReference extends ModuleReference {

      private final Path path;

      ModuleInfoReference(ModuleDescriptor descriptor, Path path) {
        super(descriptor, path.toUri());
        this.path = path;
      }

      Path path() {
        return path;
      }

      @Override
      public ModuleReader open() {
        throw new UnsupportedOperationException("Can't open a module-info.java file for reading");
      }
    }

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

    /** Find all {@code module-info.java} files below the given root directory. */
    static List<ModuleInfoReference> findModuleInfoReferences(Path root) {
      return Util.find(Set.of(root), Util::isModuleInfo).stream()
          .map(Modules::parseModuleInfo)
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
    static ModuleInfoReference parseModuleInfo(Path path) {
      if (!Util.isModuleInfo(path)) {
        throw new IllegalArgumentException("Expected module-info.java path, but got: " + path);
      }
      try {
        return new ModuleInfoReference(parseDeclaration(Files.readString(path)), path);
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
    static List<Path> find(Collection<Path> roots, Predicate<Path> filter) {
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

    /** Test supplied path for pointing to a Java binary unit. */
    static boolean isClassFile(Path path) {
      return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".class");
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

    /** Returns the size of a file in bytes. */
    static long size(Path path) {
      try {
        return Files.size(path);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    /** Sleep and silently clear current thread's interrupted status. */
    static void sleep(long millis) {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        Thread.interrupted();
      }
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

    /** Convert given path to its URL. */
    static URL url(Path path) {
      try {
        return path.toUri().toURL();
      } catch (MalformedURLException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
