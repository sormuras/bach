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

import java.io.File;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  /** Tool map. */
  final Map<String, Tool> tools;

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
    this.tools = new HashMap<>();

    tools.put("format", Tool::format);
    tools.put("junit", Tool::junit);
    tools.put("maven", Tool::maven);
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

  /** Download tool archive and return path to it. */
  private Path download(Property property) throws Exception {
    var tool = Path.of(get(Property.TOOL_HOME));
    var name = property.name().substring(9).toLowerCase(); // "TOOL_URI_XYZ" -> "xyz"
    return download(tool.resolve(name), URI.create(get(property)));
  }

  /** Download file from supplied uri to specified destination directory. */
  Path download(Path destination, URI uri) throws Exception {
    return Util.download(log::debug, Boolean.parseBoolean(get(Property.OFFLINE)), destination, uri);
  }

  /** Build all and everything. */
  public void build() throws Exception {
    log.trace("build()");
    project.assemble();
    project.main.compile();
    project.test.compile();
    project.test();
    project.jar();
  }

  /** Delete generated binary assets. */
  public void clean() throws Exception {
    log.trace("clean()");
    Util.treeDelete(project.bin);
  }

  /** Delete generated binary assets and local build cache directory. */
  public void erase() throws Exception {
    log.trace("erase()");
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

  /** Get regex-separated values of the supplied property as a stream of strings. */
  Stream<String> get(Property property, String regex) {
    var value = get(property.key, property.defaultValue);
    if (value.isBlank()) {
      return Stream.empty();
    }
    return Arrays.stream(value.split(regex)).map(String::strip);
  }

  /** Print help text to the "standard" output stream. */
  public void help() {
    log.trace("help()");
    System.out.println();
    help(System.out::println);
    System.out.println();
  }

  /** Start main program. */
  public void launch() throws Exception {
    log.trace("launch()");
    project.launch();
  }

  /** Create modular Java sample project in base directory. */
  public void scaffold() throws Exception {
    log.trace("scaffold()");
    var uri = "https://github.com/sormuras/bach/raw/" + VERSION + "/demo/scaffold.zip";
    var zip = download(base, URI.create(uri));
    Util.unzip(zip);
    Files.delete(zip);
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
    log.debug(String.format("Performing %d action(s)...", actions.size()));
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
      var command = name + (arguments.length == 0 ? "" : " " + List.of(arguments));
      throw new Error("Expected " + expected + ", but got " + actual + " as result of: " + command);
    }
  }

  /** Execute the named tool and return its exit value. */
  int run(String name, Object... arguments) {
    var args = new String[arguments.length];
    for (int i = 0; i < args.length; i++) {
      args[i] = arguments[i].toString();
    }
    log.trace(String.format("run(%s, %s)", name, List.of(args)));
    var toolProvider = ToolProvider.findFirst(name);
    if (toolProvider.isPresent()) {
      var tool = toolProvider.get();
      log.debug("Running provided tool in-process: " + tool);
      return tool.run(System.out, System.err, args);
    }
    try {
      var tool = tools.get(name);
      if (tool != null) {
        log.debug("Running mapped tool in-process: " + tool);
        tool.run(this, arguments);
        return 0;
      }
    } catch (Exception e) {
      throw new Error("Running tool " + name + " failed!", e);
    }
    // TODO Find executable via {java.home}/${name}[.exe]
    try {
      var builder = new ProcessBuilder(name);
      switch (get(Property.RUN_REDIRECT_TYPE).toUpperCase()) {
        case "INHERIT":
          log.debug("Redirect: INHERIT");
          builder.inheritIO();
          break;
        case "DISCARD":
          log.debug("Redirect: DISCARD");
          builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
          builder.redirectError(ProcessBuilder.Redirect.DISCARD);
          break;
        case "FILE":
          if (get(Property.RUN_REDIRECT_FILE).isEmpty()) {
            var temp = Files.createTempFile("bach-run-", ".txt");
            properties.setProperty(Property.RUN_REDIRECT_FILE.key, temp.toString());
          }
          var temp = Path.of(get(Property.RUN_REDIRECT_FILE));
          log.debug("Redirect: FILE " + temp);
          builder.redirectErrorStream(true);
          builder.redirectOutput(ProcessBuilder.Redirect.appendTo(temp.toFile()));
          break;
        default:
          log.debug("Redirect: PIPE");
      }
      builder.command().addAll(List.of(args));
      var process = builder.start();
      log.debug("Running tool in a new process: " + process);
      return process.waitFor();
    } catch (Exception e) {
      throw new Error("Running tool " + name + " failed!", e);
    }
  }

  /** Bach consuming no-arg action operating via side-effects. */
  @FunctionalInterface
  interface Action {

    /** Performs this action on the given Bach instance. */
    void perform(Bach bach) throws Exception;

    /** Default action delegating to Bach API methods. */
    enum Default implements Action {
      BUILD(Bach::build, "Build modular Java project in base directory."),
      CLEAN(Bach::clean, "Delete all generated assets - but keep caches intact."),
      ERASE(Bach::erase, "Delete all generated assets - and also delete caches."),
      HELP(Bach::help, "Print this help screen on standard out... F1, F1, F1!"),
      LAUNCH(Bach::launch, "Start project's main program."),
      TOOL(
          null,
          "Run named tool consuming all remaining arguments:",
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
      },
      SCAFFOLD(Bach::scaffold, "Create modular Java sample project in base directory.");

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

  /** Bach consuming parameterized action operating via side-effects. */
  @FunctionalInterface
  interface Tool {

    /** Run this tool on the given Bach instance. */
    void run(Bach bach, Object... args) throws Exception;

    /** Run format. */
    static void format(Bach bach, Object... args) throws Exception {
      bach.log.debug("format(" + List.of(args) + ")");
      var jar = bach.download(Property.TOOL_URI_FORMAT);
      var arguments = new ArrayList<>();
      arguments.add("-jar");
      arguments.add(jar);
      arguments.addAll(List.of(args));
      bach.run(0, "java", arguments.toArray(Object[]::new));
    }

    /** Run format. */
    static void format(Bach bach, boolean replace, Collection<Path> roots) throws Exception {
      // Find all .java files in given root paths...
      var files = new ArrayList<Path>();
      for (var root : roots) {
        if (Files.isDirectory(root)) {
          files.addAll(Util.findJavaFiles(root));
        }
      }
      // No .java file found? Done.
      if (files.isEmpty()) {
        return;
      }
      var args = new ArrayList<>();
      args.addAll(replace ? List.of("--replace") : List.of("--dry-run", "--set-exit-if-changed"));
      args.addAll(files);
      format(bach, args.toArray(Object[]::new));
    }

    /** Run JUnit Platform Console Launcher. */
    static void junit(Bach bach, Object... args) throws Exception {
      bach.log.debug("junit(" + List.of(args) + ")");
      junit(bach, new ArrayList<>(), args);
    }

    /** Run JUnit Platform Console Launcher. */
    static void junit(Bach bach, List<Object> java, Object... args) throws Exception {
      java.add("--class-path");
      java.add(bach.download(Property.TOOL_URI_JUNIT));
      java.add("org.junit.platform.console.ConsoleLauncher");
      java.addAll(List.of(args));
      bach.run(0, "java", java.toArray(Object[]::new));
    }

    /** Run Maven. */
    static void maven(Bach bach, Object... args) throws Exception {
      bach.log.debug("maven(" + List.of(args) + ")");
      var zip = bach.download(Property.TOOL_URI_MAVEN);
      bach.log.debug("unzip(" + zip + ")");
      var home = Bach.Util.unzip(zip);
      var win = System.getProperty("os.name").toLowerCase().contains("win");
      var name = "mvn" + (win ? ".cmd" : "");
      var executable = home.resolve("bin").resolve(name);
      executable.toFile().setExecutable(true);
      bach.run(0, executable.toString(), args);
    }
  }

  /** Property names, keys and default values. */
  enum Property {
    PROPERTIES("bach.properties"),
    BASE("."),
    LOG_LEVEL("INFO"),
    /** Offline mode flag. */
    OFFLINE("false"),
    /** Default Maven repository used for artifact resolution. */
    MAVEN_REPOSITORY("https://repo1.maven.org/maven2"),
    PROJECT_NAME("project"),
    PROJECT_VERSION("1.0.0-SNAPSHOT"),
    PROJECT_LAUNCH_MODULE("<module>[/<main-class>]"),
    PROJECT_LAUNCH_OPTIONS(""),
    RUN_REDIRECT_TYPE("INHERIT"),
    RUN_REDIRECT_FILE(""), // empty: create temporary file
    /** Home directory for downloadable tools. */
    TOOL_HOME(USER_HOME.resolve(".bach/tool").toString()),
    /** URI to Google Java Format "all-deps" JAR. */
    TOOL_URI_FORMAT(
        "https://github.com/"
            + "google/google-java-format/releases/download/google-java-format-1.7/"
            + "google-java-format-1.7-all-deps.jar"),
    /** URI to JUnit Platform Console Standalone JAR. */
    TOOL_URI_JUNIT(
        MAVEN_REPOSITORY.get()
            + "/org/junit/platform/junit-platform-console-standalone/1.4.0/"
            + "junit-platform-console-standalone-1.4.0.jar"),
    /** Maven URI. */
    TOOL_URI_MAVEN(
        "https://archive.apache.org/"
            + "dist/maven/maven-3/3.6.0/binaries/"
            + "apache-maven-3.6.0-bin.zip");

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

    /** Log trace message unless threshold suppresses it. */
    void trace(String message) {
      log.log(Level.TRACE, message);
    }

    /** Log debug message unless threshold suppresses it. */
    void debug(String message) {
      log.log(Level.DEBUG, message);
    }

    /** Log message unless threshold suppresses it. */
    void log(Level level, String message) {
      if (level.getSeverity() < threshold.getSeverity()) {
        return;
      }
      var consumer = level.getSeverity() < Level.WARNING.getSeverity() ? out : err;
      consumer.accept(message);
    }
  }

  /** Simple module information collector. */
  static class ModuleInfo {

    private static final Pattern NAME = Pattern.compile("(module)\\s+(.+)\\s*\\{.*");

    private static final Pattern PACKAGE = Pattern.compile("package\\s+(.+?);", Pattern.DOTALL);

    private static final Pattern REQUIRES = Pattern.compile("requires (.+?);", Pattern.DOTALL);

    private static final Pattern TYPE = Pattern.compile("(class|interface|enum)\\s+(.+)\\s*\\{.*");

    static ModuleInfo of(Path path) {
      if (Files.isDirectory(path)) {
        path = path.resolve("module-info.java");
      }
      try {
        return of(Files.readString(path));
      } catch (Exception e) {
        throw new RuntimeException("reading '" + path + "' failed", e);
      }
    }

    static ModuleInfo of(String source) {
      // extract module name
      var nameMatcher = NAME.matcher(source);
      if (!nameMatcher.find()) {
        throw new IllegalArgumentException(
            "expected java module descriptor unit, but got: " + source);
      }
      var name = nameMatcher.group(2).trim();

      // extract required module names
      var requiresMatcher = REQUIRES.matcher(source);
      var requires = new TreeSet<String>();
      while (requiresMatcher.find()) {
        var split = requiresMatcher.group(1).trim().split("\\s+");
        requires.add(split[split.length - 1]);
      }
      return new ModuleInfo(name, requires);
    }

    /** Enumerate all system module names. */
    static Set<String> findSystemModuleNames() {
      return ModuleFinder.ofSystem().findAll().stream()
          .map(reference -> reference.descriptor().name())
          .collect(Collectors.toSet());
    }

    /** Calculate external module names. */
    static Set<String> findExternalModuleNames(Set<Path> roots) {
      var declaredModules = new TreeSet<String>();
      var requiredModules = new TreeSet<String>();
      var paths = new ArrayList<Path>();
      for (var root : roots) {
        try (var stream = Files.walk(root)) {
          stream.filter(path -> path.endsWith("module-info.java")).forEach(paths::add);
        } catch (Exception e) {
          throw new RuntimeException("walking path failed for: " + root, e);
        }
      }
      for (var path : paths) {
        var info = ModuleInfo.of(path);
        declaredModules.add(info.name);
        requiredModules.addAll(info.requires);
      }
      var externalModules = new TreeSet<>(requiredModules);
      externalModules.removeAll(declaredModules);
      externalModules.removeAll(findSystemModuleNames()); // "java.base", "java.logging", ...
      return externalModules;
    }

    /** Find first Java program walking root path or {@code null}. */
    static String findProgram(Path root) throws Exception {
      var programs = findPrograms(root, true);
      return programs.isEmpty() ? null : programs.get(0);
    }

    /** Find first or all Java programs walking root path. */
    static List<String> findPrograms(Path root, boolean first) throws Exception {
      var programs = new ArrayList<String>();
      try (var stream = Files.walk(root)) {
        for (var path : stream.filter(Util::isJavaFile).collect(Collectors.toList())) {
          var source = Files.readString(path);
          // TODO Replace hard-coded search sequence with a regular expression.
          if (source.contains("static void main(String")) {
            // extract name from "module-info.java" in parent directories
            var modulePath = path.getParent();
            while (modulePath != null) {
              if (Files.exists(modulePath.resolve("module-info.java"))) {
                break;
              }
              modulePath = modulePath.getParent();
            }
            if (modulePath == null) {
              throw new IllegalStateException("expected 'module-info.java' in parents of " + path);
            }
            var moduleName = ModuleInfo.of(modulePath).name;
            // extract name of type's package
            var packageMatcher = PACKAGE.matcher(source);
            if (!packageMatcher.find()) {
              throw new IllegalStateException("expected package to be declared in " + path);
            }
            var packageName = packageMatcher.group(1);
            // extract name of the type
            var typeMatcher = TYPE.matcher(source);
            if (!typeMatcher.find()) {
              throw new IllegalStateException("expected java compilation unit, but got: " + path);
            }
            var typeName = typeMatcher.group(2).trim().split(" ")[0];
            var program = moduleName + '/' + packageName + '.' + typeName;
            programs.add(program);
            if (first) {
              break;
            }
          }
        }
      }
      return programs;
    }

    final String name;
    final Set<String> requires;

    private ModuleInfo(String name, Set<String> requires) {
      this.name = name;
      this.requires = Set.copyOf(requires);
    }
  }

  /** Project model. */
  final class Project {
    /** Destination directory for generated binaries. */
    final Path bin;
    /** Root of local build cache. */
    final Path cache;
    /** Locally cached modules. */
    final Path cachedModules;
    /** User-managed 3rd-party libraries. */
    final Path lib;
    /** Name of the project. */
    final String name;
    /** Main realm. */
    final Realm main;
    /** Test realm. */
    final Realm test;
    /** Version of the project. */
    final String version;

    /** Initialize project properties with default values. */
    Project() {
      this.bin = based("bin");
      this.cache = based(".bach");
      this.cachedModules = cache.resolve("modules");
      this.lib = based("lib");
      this.name =
          get(
              Property.PROJECT_NAME.key,
              base.getNameCount() > 0
                  ? base.toAbsolutePath().getFileName() + ""
                  : Property.PROJECT_NAME.defaultValue);
      this.version = get(Property.PROJECT_VERSION);
      // Realm: "main"
      this.main =
          new Realm(
              "main",
              findFirst("src/main/java", "src/main", "main", "src"),
              Util.join(lib, cachedModules),
              Map.of());
      // Realm: "test"
      var testSource = findFirst("src/test/java", "src/test", "test");
      this.test =
          new Realm(
              "test",
              testSource,
              Util.join(main.target, lib, cachedModules),
              Util.findPatchMap(List.of(testSource), List.of(main.source)));
    }

    /** Assemble all assets. */
    void assemble() throws Exception {
      log.debug("assemble()");
      Tool.format(Bach.this, false, Set.of(main.source, test.source));
      assembleExternalModules();
    }

    /** Assemble external modules. */
    void assembleExternalModules() throws Exception {
      // TODO get("bach.project.modules.uris", "", ",")
      //              .map(URI::create)
      //              .peek(uri -> log.debug("Loading %s", uri))
      //              .forEach(uri -> new Tool.Download(uri, modules).apply(Bach.this));
      var roots =
          Set.of(main.source, test.source).stream()
              .filter(Files::isDirectory)
              .collect(Collectors.toSet());
      var externals = ModuleInfo.findExternalModuleNames(roots);
      if (externals.isEmpty()) {
        return;
      }
      log.debug("External module names: " + externals);
      var moduleMaven =
          Property.loadProperties(
              download(
                  cache,
                  URI.create(
                      "https://raw.githubusercontent.com/"
                          + "sormuras/modules/master/"
                          + "module-maven.properties")));
      var moduleVersion =
          Property.loadProperties(
              download(
                  cache,
                  URI.create(
                      "https://raw.githubusercontent.com/"
                          + "sormuras/modules/master/"
                          + "module-version.properties")));
      var uris = new ArrayList<URI>();
      for (var external : externals) {
        var uri = get("module." + external, null);
        if (uri != null) {
          log.debug(String.format("External module %s mapped to custom uri: %s", external, uri));
          uris.add(URI.create(uri));
          continue;
        }
        var mavenGA = moduleMaven.getProperty(external);
        if (mavenGA == null) {
          log.log(Level.WARNING, String.format("External module not mapped: %s", external));
          continue;
        }
        var group = mavenGA.substring(0, mavenGA.indexOf(':'));
        var artifact = mavenGA.substring(group.length() + 1);
        var version = moduleVersion.getProperty(external);
        uris.add(maven(group, artifact, version));
      }
      for (var uri : uris) {
        var path = download(cachedModules, uri);
        log.debug("Resolved " + path);
      }
    }

    /** Create URI for supplied Maven coordinates. */
    URI maven(String group, String artifact, String version) {
      // TODO Try local repository first?
      // Path.of(System.getProperty("user.home"), ".m2", "repository").toUri().toString();
      var repo = get(Property.MAVEN_REPOSITORY);
      var file = artifact + "-" + version + ".jar";
      return URI.create(String.join("/", repo, group.replace('.', '/'), artifact, version, file));
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

    Path findFirst(String... paths) {
      return Arrays.stream(paths)
          .map(Project.this::based)
          .filter(Files::isDirectory)
          .findFirst()
          .orElse(based(paths[0]));
    }

    /** Launch main program. */
    void launch() throws Exception {
      if (Files.notExists(main.target)) {
        log.log(Level.INFO, "Skip launch. No compiled classes target found: " + main.target);
        return;
      }
      var defaultLaunch = ModuleInfo.findProgram(main.source);
      var launch = get(Property.PROJECT_LAUNCH_MODULE.key, defaultLaunch);
      if (launch == null) {
        log.log(Level.INFO, "No <module>[/<main-class>] supplied, no launch.");
        return;
      }
      log.log(Level.INFO, "Launching " + launch + "...");
      var java = new ArrayList<>();
      get(Property.PROJECT_LAUNCH_OPTIONS, "\\|").forEach(java::add);
      java.add("--module-path");
      java.add(Util.join(main.target, lib, cachedModules));
      java.add("--module");
      java.add(launch);
      run(0, "java", java.toArray(Object[]::new));
    }

    /** Start test run. */
    void test() throws Exception {
      if (Files.notExists(test.target)) {
        log.log(Level.INFO, "Skip test. No compiled classes target found: " + test.target);
        return;
      }
      log.log(Level.INFO, "Launching JUnit Platform...");
      var java = new ArrayList<>();
      java.add("--module-path");
      java.add(Util.join(test.target, lib, cachedModules));
      java.add("--add-modules");
      java.add(String.join(",", Util.findDirectoryNames(test.target))); // "ALL-MODULE-PATH"?
      Tool.junit(Bach.this, java, "--reports-dir", bin.resolve("test-reports"), "--scan-modules");
    }

    void jar() {
      if (Files.notExists(main.target)) {
        log.log(Level.INFO, "Skip jar. No compiled classes target found: " + main.target);
        return;
      }
      log.log(Level.INFO, "Jarring main module(s)...");
      for (var directory : Util.findDirectoryNames(main.target)) {
        var file = bin.resolve(directory + "@" + version + ".jar");
        var jar = new ArrayList<>();
        jar.add("--create");
        jar.add("--file");
        jar.add(file);
        jar.add("-C");
        jar.add(main.target.resolve(directory));
        jar.add(".");
        run(0, "jar", jar.toArray(Object[]::new));
        run(0, "jar", "--describe-module", "--file", file);
      }
    }

    void link() throws Exception {
      if (Files.notExists(main.target)) {
        log.log(Level.INFO, "Skip link. No compiled classes target found: " + main.target);
        return;
      }
      var output = bin.resolve(name);
      if (Files.exists(output)) {
        log.debug("Deleting existing custom runtime image...");
        Util.treeDelete(output);
      }
      log.log(Level.INFO, "Creating custom runtime image...");
      var link = new ArrayList<>();
      link.add("--output");
      link.add(output);
      link.add("--strip-debug");
      link.add("--compress=2");
      var defaultLaunch = ModuleInfo.findProgram(main.source);
      var launch = get(Property.PROJECT_LAUNCH_MODULE.key, defaultLaunch);
      if (launch != null) {
        link.add("--launcher");
        link.add(name + "=" + launch);
      }
      link.add("--module-path");
      link.add(String.join(File.pathSeparator, main.target.toString(), main.modulePath));
      for (var directory : Util.findDirectoryNames(main.target)) {
        link.add("--add-modules");
        link.add(directory);
      }
      run(0, "jlink", link.toArray(Object[]::new));
    }

    /** Building block, source set, scope, directory, named context: {@code main}, {@code test}. */
    class Realm {
      /** Name of the realm. */
      final String name;
      /** Source path. */
      final Path source;
      /** Target path. */
      final Path target;
      /** Module path. */
      final String modulePath;
      /** Patch modules. */
      final Map<String, Set<Path>> patches;

      /** Initialize this realm. */
      Realm(String name, Path source, String modulePath, Map<String, Set<Path>> patches) {
        this.name = name;
        this.source = source;
        this.target = bin.resolve(Path.of("realm", name));
        this.modulePath = modulePath;
        this.patches = patches;
      }

      /** Compile all Java sources found in this realm. */
      void compile() throws Exception {
        log.trace(String.format("%s.compile()", name));
        if (Files.notExists(source)) {
          log.log(Level.INFO, String.format("Skip %s.compile(): path %s not found", name, source));
          return;
        }
        var javac = new ArrayList<>();
        javac.add("-d");
        javac.add(target);
        javac.add("--module-path");
        javac.add(modulePath);
        javac.add("--module-source-path");
        javac.add(source);
        for (var entry : patches.entrySet()) {
          javac.add("--patch-module");
          javac.add(entry.getKey() + "=" + Util.join(entry.getValue()));
        }
        javac.addAll(Util.findJavaFiles(source));
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

    /** Download file from supplied uri to specified destination directory. */
    static Path download(Consumer<String> logger, boolean offline, Path destination, URI uri)
        throws Exception {
      logger.accept("download(" + uri + ")");
      var fileName = extractFileName(uri);
      var target = Files.createDirectories(destination).resolve(fileName);
      var url = uri.toURL();
      if (offline) {
        if (Files.exists(target)) {
          logger.accept("Offline mode is active and target already exists.");
          return target;
        }
        throw new IllegalStateException("Target is missing and being offline: " + target);
      }
      var connection = url.openConnection();
      try (var sourceStream = connection.getInputStream()) {
        var millis = connection.getLastModified();
        var lastModified = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
        if (Files.exists(target)) {
          logger.accept("Local target file exists. Comparing last modified timestamps...");
          var fileModified = Files.getLastModifiedTime(target);
          logger.accept(" o Remote Last Modified -> " + lastModified);
          logger.accept(" o Target Last Modified -> " + fileModified);
          if (fileModified.equals(lastModified)) {
            logger.accept(String.format("Already downloaded %s previously.", fileName));
            return target;
          }
          logger.accept("Local target file differs from remote source -- replacing it...");
        }
        logger.accept("Transferring " + uri);
        try (var targetStream = Files.newOutputStream(target)) {
          sourceStream.transferTo(targetStream);
        }
        Files.setLastModifiedTime(target, lastModified);
        logger.accept(String.format(" o Remote   -> %s", uri));
        logger.accept(String.format(" o Target   -> %s", target.toUri()));
        logger.accept(String.format(" o Modified -> %s", lastModified));
        logger.accept(String.format(" o Size     -> %d bytes", Files.size(target)));
        logger.accept(String.format("Downloaded %s successfully.", fileName));
      }
      return target;
    }

    /** Extract last path element from the supplied uri. */
    static String extractFileName(URI uri) {
      var path = uri.getPath(); // strip query and fragment elements
      return path.substring(path.lastIndexOf('/') + 1);
    }

    /** Return list of child directories directly present in {@code root} path. */
    static List<Path> findDirectories(Path root) {
      if (Files.notExists(root)) {
        return List.of();
      }
      try (var paths = Files.find(root, 1, (path, attr) -> Files.isDirectory(path))) {
        return paths.filter(path -> !root.equals(path)).collect(Collectors.toList());
      } catch (Exception e) {
        throw new Error("findDirectories failed for root: " + root, e);
      }
    }

    /** Return list of child directory names directly present in {@code root} path. */
    static List<String> findDirectoryNames(Path root) {
      return findDirectories(root).stream()
          .map(root::relativize)
          .map(Path::toString)
          .collect(Collectors.toList());
    }

    /** Return patch map using two collections of paths. */
    static Map<String, Set<Path>> findPatchMap(Collection<Path> bases, Collection<Path> patches) {
      var map = new TreeMap<String, Set<Path>>();
      for (var base : bases) {
        for (var name : findDirectoryNames(base)) {
          for (var patch : patches) {
            var candidate = patch.resolve(name);
            if (Files.isDirectory(candidate)) {
              map.computeIfAbsent(name, __ -> new TreeSet<>()).add(candidate);
            }
          }
        }
      }
      return map;
    }

    /** List all regular files matching the given filter. */
    static List<Path> findFiles(Collection<Path> roots, Predicate<Path> filter) throws Exception {
      var files = new ArrayList<Path>();
      for (var root : roots) {
        try (var stream = Files.walk(root)) {
          stream.filter(Files::isRegularFile).filter(filter).forEach(files::add);
        }
      }
      return files;
    }

    /** List all regular Java files in given root directory. */
    static List<Path> findJavaFiles(Path root) throws Exception {
      return findFiles(List.of(root), Util::isJavaFile);
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

    /** Join supplied paths into a single string joined by current path separator. */
    static String join(Collection<?> paths) {
      return paths.stream().map(Object::toString).collect(Collectors.joining(File.pathSeparator));
    }

    /** Join supplied paths into a single string. joined by current path separator. */
    static String join(Object path, Object... more) {
      if (more.length == 0) {
        return path.toString();
      }
      var strings = new String[1 + more.length];
      strings[0] = path.toString();
      for (var i = 0; i < more.length; i++) {
        strings[1 + i] = more[i].toString();
      }
      return String.join(File.pathSeparator, strings);
    }

    /** Copy all files and directories from source to target directory. */
    static void treeCopy(Path source, Path target) throws Exception {
      treeCopy(source, target, __ -> true);
    }

    /** Copy selected files and directories from source to target directory. */
    static void treeCopy(Path source, Path target, Predicate<Path> filter) throws Exception {
      // debug("treeCopy(source:`%s`, target:`%s`)%n", source, target);
      if (!Files.exists(source)) {
        throw new IllegalArgumentException("source must exist: " + source);
      }
      if (!Files.isDirectory(source)) {
        throw new IllegalArgumentException("source must be a directory: " + source);
      }
      if (Files.exists(target)) {
        if (!Files.isDirectory(target)) {
          throw new IllegalArgumentException("target must be a directory: " + target);
        }
        if (target.equals(source)) {
          return;
        }
        if (target.startsWith(source)) {
          // copy "a/" to "a/b/"...
          throw new IllegalArgumentException("target must not a child of source");
        }
      }
      try (var stream = Files.walk(source).sorted()) {
        var paths = stream.collect(Collectors.toList());
        for (var path : paths) {
          var destination = target.resolve(source.relativize(path).toString());
          var lastModified = Files.getLastModifiedTime(path);
          if (Files.isDirectory(path)) {
            Files.createDirectories(destination);
            Files.setLastModifiedTime(destination, lastModified);
            continue;
          }
          if (filter.test(path)) {
            if (Files.exists(destination)) {
              if (lastModified.equals(Files.getLastModifiedTime(destination))) {
                continue;
              }
            }
            Files.copy(
                path,
                destination,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES);
          }
        }
      }
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

    /** Unzip file "in place". */
    static Path unzip(Path zip) throws Exception {
      return unzip(zip, zip.toAbsolutePath().getParent());
    }

    /** Unzip file to specified destination directory. */
    static Path unzip(Path zip, Path destination) throws Exception {
      var loader = Bach.class.getClassLoader();
      try (var zipFileSystem = FileSystems.newFileSystem(zip, loader)) {
        var root = zipFileSystem.getPath(zipFileSystem.getSeparator());
        treeCopy(root, destination);
        // Single subdirectory in root of the zip file?
        var stream = Files.list(root);
        var entries = stream.collect(Collectors.toList());
        if (entries.size() == 1) {
          var singleton = entries.get(0);
          if (Files.isDirectory(singleton)) {
            return destination.resolve(singleton.getFileName().toString());
          }
        }
      }
      return destination;
    }
  }
}
