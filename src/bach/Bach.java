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
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Java Shell Builder. */
class Bach {

  /** Version tag, defaults to {@code master}. */
  static final String VERSION = "master";

  /** User's current working directory as path. */
  static final Path USER_PATH = Path.of(System.getProperty("user.dir"));

  /** Main entry-point. */
  public static void main(String... args) throws Exception {
    System.out.println(
        "    ___      ___      ___      ___   \n"
            + "   /\\  \\    /\\  \\    /\\  \\    /\\__\\  \n"
            + "  /::\\  \\  /::\\  \\  /::\\  \\  /:/__/_ \n"
            + " /::\\:\\__\\/::\\:\\__\\/:/\\:\\__\\/::\\/\\__\\\n"
            + " \\:\\::/  /\\/\\::/  /\\:\\ \\/__/\\/\\::/  /\n"
            + "  \\::/  /   /:/  /  \\:\\__\\    /:/  / \n"
            + "   \\/__/    \\/__/    \\/__/    \\/__/"
            + " "
            + VERSION
            + "\n\n");
    var bach = new Bach(args);
    var code = bach.run();
    if (code != 0) {
      throw new Error("Bach finished with exit code " + code);
    }
  }

  final Path base;
  final List<String> arguments;
  final Properties properties;
  final Log log;
  final Var var;
  final Project project;

  Bach(String... arguments) {
    this(Path.of(System.getProperty("bach.base", System.getProperty("user.dir"))), arguments);
  }

  Bach(Path base, String... arguments) {
    this(base, Util.loadProperties(base.resolve("bach.properties")), List.of(arguments));
  }

  Bach(Path base, Properties properties, List<String> arguments) {
    this.base = base;
    this.properties = properties;
    this.arguments = arguments;
    this.log = new Log();
    this.var = new Var();
    this.project = new Project();
  }

  Path based(Path path) {
    if (path.isAbsolute()) {
      return path;
    }
    if (base.equals(USER_PATH)) {
      return path;
    }
    return base.resolve(path);
  }

  Path based(String first, String... more) {
    return based(Path.of(first, more));
  }

  Path based(Property property) {
    return based(Path.of(get(property)));
  }

  /** Get the value of the supplied property. */
  String get(Property property) {
    return get(property.key, property.defaultValue);
  }

  /** Get the entire configured string value of the supplied key. */
  String get(String key, String defaultValue) {
    return System.getProperty(key, properties.getProperty(key, defaultValue));
  }

  /** Get configured regex-separated values of the supplied key as a stream of strings. */
  Stream<String> get(String key, String defaultValue, String regex) {
    var value = get(key, defaultValue);
    if (value.isBlank()) {
      return Stream.empty();
    }
    return Arrays.stream(value.split(regex)).map(String::strip);
  }

  /** Main entry-point entry-point. */
  int run() throws ReflectiveOperationException {
    // Welcome!
    log.info("Bach - %s - [%s]", VERSION, base);

    // Debug...
    log.debug("Arguments");
    if (arguments.isEmpty()) {
      log.debug("<none>");
    } else {
      for (var argument : arguments) {
        log.debug("  -> %s", argument);
      }
    }
    log.debug("Variables");
    for (var var : Var.class.getDeclaredFields()) {
      log.debug("  %s -> %s", var.getName(), var.get(this.var));
    }
    log.debug("Properties");
    for (var property : Property.values()) {
      log.debug("  %s -> %s", property.key, get(property));
    }
    log.debug("Project");
    log.debug("  name -> %s", project.name);
    log.debug("  version -> %s", project.version);
    log.debug("  source -> %s", project.source);
    log.debug("  target -> %s", project.target);
    log.debug("  launch -> %s", project.launch);
    for (var realm : List.of(project.main, project.test)) {
      log.debug("Project Realm '%s'", realm.name);
      for (var field : Project.Realm.class.getDeclaredFields()) {
        log.debug("  %s.%s -> %s", realm.name, field.getName(), field.get(realm));
      }
    }

    // Action!
    if (arguments.isEmpty()) {
      var action = Action.valueOf(get(Property.ACTION).toUpperCase());
      log.debug("Calling default action: %s", action.name());
      return action.apply(this);
    } else {
      var code = 0;
      for (var argument : arguments) {
        var action = Action.valueOf(argument.toUpperCase());
        log.debug("Calling action: %s", action.name());
        code += action.apply(this);
        if (code != 0 && var.failFast) {
          log.log(Level.ERROR, "Action " + action + " returned " + code);
          return code;
        }
        if (action == Action.TOOL) {
          // all arguments are consumed by the external tool
          break;
        }
      }
      return code;
    }
  }

  /** Run named executable with given arguments. */
  int run(String executable, Object... arguments) {
    log.info("[run] %s %s", executable, List.of(arguments));
    return new Command(executable).addAll(arguments).apply(this);
  }

  /** Run function. */
  int run(String caption, Function<Bach, Integer> function) {
    return run(caption, () -> function.apply(this));
  }

  /** Run function using its simple class name as the caption. */
  int run(Function<Bach, Integer> function) {
    return run(function.getClass().getSimpleName(), function);
  }

  /** Run tasks in parallel. */
  @SafeVarargs
  final int run(String caption, Supplier<Integer>... tasks) {
    return run(caption, Stream.of(tasks).parallel());
  }

  /** Run stream of tasks. */
  int run(String caption, Stream<Supplier<Integer>> stream) {
    log.info("[run] %s...", caption);
    var results = stream.map(CompletableFuture::supplyAsync).map(CompletableFuture::join);
    var result = results.reduce(0, Integer::sum);
    log.info("[run] %s done.", caption);
    return result;
  }

  @Override
  public String toString() {
    return "Bach - " + VERSION + " - @" + Integer.toHexString(System.identityHashCode(this));
  }

  /** Logging support. */
  class Log {
    /** Logger function defaults to {@linkplain #log(Level, String)} of this class. */
    BiConsumer<Level, String> logger = this::log;

    /** Logger format in {@link java.util.Formatter} style. */
    String format = get(Property.LOG_FORMAT);

    /** Current logger level threshold. */
    Level level = Level.valueOf(get(Property.LOG_LEVEL));

    /** Return {@code true} if debugging is enabled. */
    boolean debug() {
      return is(Level.DEBUG);
    }

    /** Return {@code true} if the supplied level is enabled. */
    boolean is(Level level) {
      return this.level.getSeverity() <= level.getSeverity();
    }

    /** Log debug level message. */
    void debug(String format, Object... arguments) {
      var text = arguments.length == 0 ? format : String.format(format, arguments);
      logger.accept(Level.DEBUG, text);
    }

    /** Log info level message. */
    void info(String format, Object... arguments) {
      var text = arguments.length == 0 ? format : String.format(format, arguments);
      logger.accept(Level.INFO, text);
    }

    /** Log the supplied text as-is to standard streams, unless the supplied level is muted. */
    void log(Level level, String text) {
      var severity = level.getSeverity();
      if (severity < this.level.getSeverity()) {
        return;
      }
      var error = severity >= Level.ERROR.getSeverity();
      var stream = error ? var.streamErr : var.streamOut;
      stream.printf(format, level, text);
    }
  }

  /** Variables. */
  class Var {
    /** Return non-zero exit code on first failed action/function/operation. */
    boolean failFast = Boolean.valueOf(get(Property.FAIL_FAST));

    /** Use only locally cached assets. */
    boolean offline = Boolean.valueOf(get(Property.OFFLINE));

    /** Print stream to emit error messages to. */
    PrintStream streamErr = System.err;

    /** Print stream to emit standard messages to. */
    PrintStream streamOut = System.out;
  }

  /** Project. */
  class Project {

    /** Building block, source set, scope, directory, named context: {@code main}, {@code test}. */
    class Realm {
      final String name;
      final Layout layout;
      final Path source;
      final Path target;

      Realm(String name) {
        this.name = name;
        this.source = based(get(Property.PATH_SOURCE) + "/" + name + "/java");
        this.target = based(get(Property.PATH_TARGET) + "/compiled/" + name);
        this.layout = Layout.of(source);
      }

      int compile() {
        if (Files.notExists(source)) {
          log.info("Skip compile for %s! None source path exists: %s", name, source);
          return 0;
        }
        log.info("Compiling %s realm...", name);
        var javac = new Command("javac");
        javac.add("-d").add(target);
        javac.add("--module-path").add(modules);
        if (layout == Layout.BASIC) {
          javac.add("--module-source-path").add(source);
        }
        get("bach.project.realms[" + name + "].compile.options", "", ",").forEach(javac::add);
        javac.mark(99);
        javac.addAllJavaFiles(Set.of(source));
        return javac.apply(Bach.this);
      }
    }

    class TestRealm extends Realm {
      TestRealm(String name) {
        super(name);
      }

      int compile() {
        if (Files.notExists(source)) {
          log.info("Skip compile for %s! None source path exists: %s", name, source);
          return 0;
        }
        log.info("Compiling %s realm...", name);
        var javac = new Command("javac");
        javac.add("-d").add(target);

        var modulePath = new ArrayList<Path>();
        if (main.layout == Layout.BASIC) {
          for (var mod : Util.findDirectoryNames(main.target)) {
            modulePath.add(main.target.resolve(mod));
          }
        } else {
          modulePath.add(main.target);
        }
        modulePath.add(modules);
        javac.add("--module-path").add(modulePath);

        var patchMap = Util.findPatchMap(List.of(test.source), List.of(main.source));

        for (var entry : patchMap.entrySet()) {
          var module = entry.getKey();
          var paths = entry.getValue();
          if (paths.isEmpty()) {
            throw new AssertionError("expected at least one patch path entry for " + module);
          }
          var patches =
              paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
          javac.add("--patch-module");
          javac.add(module + "=" + patches);
        }

        if (layout == Layout.BASIC) {
          javac.add("--module-source-path").add(source);
        }
        get("bach.project.realms[" + name + "].compile.options", "", ",").forEach(javac::add);
        javac.mark(99);
        javac.addAllJavaFiles(Set.of(source));
        return javac.apply(Bach.this);
      }
    }

    final String name, version;
    final String launch;
    final Path source, target, modules;
    final Realm main, test;

    Project() {
      this.name = get(Property.PROJECT_NAME.key, Util.last(base).toString());
      this.version = get(Property.PROJECT_VERSION);
      this.launch = get(Property.PROJECT_LAUNCH);
      this.source = based(get(Property.PATH_SOURCE));
      this.target = based(get(Property.PATH_TARGET));
      this.modules = target.resolve("modules");

      this.main = new Realm("main");
      this.test = new TestRealm("test");
    }

    int build() {
      log.info("Project '%s %s' build started...", name, version);
      var start = Instant.now();
      var code = assemble();
      code += compile();
      if (code != 0) {
        log.log(Level.WARNING, "compile() failed: " + code);
        return code;
      }
      // code += format();
      // code += jar();
      code += test();
      code += launch();
      // code += verify();
      if (code == 0) {
        var duration = Duration.between(start, Instant.now()).toMillis();
        log.info("Project '%s %s' built successfully in %d ms.", name, version, duration);
      } else {
        log.log(Level.WARNING, "Build failed!");
      }
      return code;
    }

    int assemble() {
      log.info("Assembling %s realm...", name);

      // Initialize external assets shared by all projects
      new Tool.Download(
              URI.create(
                  "https://raw.githubusercontent.com/jodastephen/jpms-module-names/master/generated/module-maven.properties"),
              based(Property.PATH_CACHE_MODULES))
          .apply(Bach.this);
      new Tool.Download(
              URI.create(
                  "https://raw.githubusercontent.com/jodastephen/jpms-module-names/master/generated/module-version.properties"),
              based(Property.PATH_CACHE_MODULES))
          .apply(Bach.this);

      var moduleMaven =
          Util.loadProperties(
              based(Property.PATH_CACHE_MODULES).resolve("module-maven.properties"));
      var moduleVersion =
          Util.loadProperties(
              based(Property.PATH_CACHE_MODULES).resolve("module-version.properties"));

      get("bach.project.modules.uris", "", ",")
          .map(URI::create)
          .peek(uri -> log.debug("Loading %s", uri))
          .forEach(uri -> new Tool.Download(uri, modules).apply(Bach.this));

      var roots =
          Set.of(main.source, test.source).stream()
              .filter(Files::isDirectory)
              .collect(Collectors.toSet());
      var externals = ModuleInfo.findExternalModuleNames(roots);
      log.info("External module names: %s", externals);
      for (var external : externals) {
        var mavenGA = moduleMaven.getProperty(external);
        if (mavenGA == null) {
          log.log(Level.WARNING, "External module not mapped: " + external);
          continue;
        }
        var group = mavenGA.substring(0, mavenGA.indexOf(':'));
        var artifact = mavenGA.substring(group.length() + 1);
        var version = moduleVersion.getProperty(external);
        var uri = Util.maven(group, artifact, version);
        log.info("  o %s:%s -> %s", mavenGA, version, uri);
        new Tool.Download(uri, modules).apply(Bach.this);
      }
      return 0;
    }

    int compile() {
      return main.compile() + test.compile();
    }

    int launch() {
      if (Files.notExists(main.target)) {
        log.info("No main target, no launch.");
        return 0;
      }
      if ("n/a".equals(launch)) {
        log.debug("%s's launch entry-point not specified", name);
        return 0;
      }
      log.info("Launching %s...", launch);
      var java = new Command("java");
      java.add("--module-path").add(List.of(main.target, modules));
      java.add("--module").add(launch);
      return java.apply(Bach.this);
    }

    int test() {
      if (Files.notExists(test.target)) {
        log.info("No test realm available, no tests.");
        return 0;
      }
      log.info("Testings...");
      var java = new Command("java");
      java.add("--module-path").add(List.of(test.target, main.target, modules));

      // java.add("--add-modules").add("ALL-MODULE-PATH,ALL-DEFAULT");
      java.add("--add-modules").add(String.join(",", Util.findDirectoryNames(test.target)));

      // java.add("--module").add("org.junit.platform.console");
      java.add("--class-path").add(Tool.JUnit.install(Bach.this));
      java.add("org.junit.platform.console.ConsoleLauncher");

      java.add("--scan-modules");
      return java.apply(Bach.this);
    }
  }
}

/** Main program actions. */
enum Action implements Function<Bach, Integer> {
  BUILD("Build project in base directory.") {
    @Override
    public Integer apply(Bach bach) {
      return bach.project.build();
    }
  },

  CLEAN("Delete all generated assets - but keep caches intact.") {
    @Override
    public Integer apply(Bach bach) {
      var target = bach.based(Property.PATH_TARGET);
      if (Files.exists(target)) {
        Util.removeTree(target);
      }
      return 0;
    }
  },

  ERASE("Delete all generated assets - and also delete caches.") {
    @Override
    public Integer apply(Bach bach) {
      var tools = bach.based(Property.PATH_CACHE_TOOLS);
      if (Files.exists(tools)) {
        Util.removeTree(tools);
      }
      CLEAN.apply(bach);
      return 0;
    }
  },

  FAIL("Set exit code to an non-zero value to fail the run.") {
    @Override
    public Integer apply(Bach bach) {
      var code = Util.integer(bach.get(Property.FAIL_CODE), -1);
      bach.log.log(Level.WARNING, "Setting exit code to " + code);
      return code;
    }
  },

  HELP("Display help screen ... F1, F1, F1!") {
    @Override
    public Integer apply(Bach bach) {
      var out = bach.var.streamOut;
      out.println();
      for (var action : Action.values()) {
        out.println(String.format(" %-9s -> %s", action.name().toLowerCase(), action.description));
      }
      out.println();
      return 0;
    }
  },

  SCAFFOLD("Create a starter project in current directory.") {
    @Override
    public Integer apply(Bach bach) {
      bach.log.log(Level.WARNING, name() + " isn't implemented, yet");
      return 0;
    }
  },

  TOOL("Execute named tool consuming all remaining actions as arguments.") {
    @Override
    public Integer apply(Bach bach) {
      var args = new LinkedList<>(bach.arguments);
      if (args.size() < 2) {
        bach.log.log(Level.WARNING, "Too few arguments for executing an external tool: " + args);
        bach.log.log(Level.WARNING, "  Usage: java Bach.java tool <name> [<arg>]*" + args);
        return 1;
      }
      args.removeFirst(); // discard "TOOL" action marker
      var name = args.removeFirst();
      try {
        var tool = Tool.of(name, args);
        return tool.apply(bach);
      } catch (UnsupportedOperationException e) {
        return bach.run(name, args.toArray());
      }
    }
  };

  final String description;

  Action(String... description) {
    this.description = String.join("", description);
  }

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}

/** Command line program and in-process tool abstraction. */
class Command implements Function<Bach, Integer> {

  final String executable;
  final List<String> arguments = new ArrayList<>();
  private int dumpLimit = Integer.MAX_VALUE;
  private int dumpOffset = Integer.MAX_VALUE;

  private Map<String, ToolProvider> tools = Collections.emptyMap();
  private boolean executableSupportsArgumentFile = false;
  private UnaryOperator<String> executableToProgramOperator = UnaryOperator.identity();
  private Path temporaryDirectory = Path.of(System.getProperty("java.io.tmpdir"));

  /** Initialize this command instance. */
  Command(String executable) {
    this.executable = executable;
    Util.findJdkCommandPath(executable)
        .ifPresent(path -> setExecutableToProgramOperator(__ -> path.toString()));
  }

  /** Add single argument composed of joined path names using {@link File#pathSeparator}. */
  Command add(Collection<Path> paths) {
    return add(paths.stream(), File.pathSeparator);
  }

  /** Add single non-null argument. */
  Command add(Object argument) {
    arguments.add(argument.toString());
    return this;
  }

  /** Add single argument composed of all stream elements joined by specified separator. */
  Command add(Stream<?> stream, String separator) {
    return add(stream.map(Object::toString).collect(Collectors.joining(separator)));
  }

  /** Add all arguments by invoking {@link #add(Object)} for each element. */
  Command addAll(Object... arguments) {
    for (var argument : arguments) {
      add(argument);
    }
    return this;
  }

  /** Add all arguments by invoking {@link #add(Object)} for each element. */
  Command addAll(Iterable<?> arguments) {
    arguments.forEach(this::add);
    return this;
  }

  /** Add all files visited by walking specified root path recursively. */
  Command addAll(Path root, Predicate<Path> predicate) {
    try (var stream = Files.walk(root).filter(predicate)) {
      stream.forEach(this::add);
    } catch (IOException e) {
      throw new UncheckedIOException("walking path `" + root + "` failed", e);
    }
    return this;
  }

  /** Add all files visited by walking specified root paths recursively. */
  Command addAll(Collection<Path> roots, Predicate<Path> predicate) {
    for (var root : roots) {
      if (Files.notExists(root)) {
        continue;
      }
      addAll(root, predicate);
    }
    return this;
  }

  /** Add all .java source files by walking specified root paths recursively. */
  Command addAllJavaFiles(Collection<Path> roots) {
    return addAll(roots, Util::isJavaFile);
  }

  /** Dump command executables and arguments using the provided string consumer. */
  Command dump(Consumer<String> consumer) {
    var iterator = arguments.listIterator();
    consumer.accept(executable);
    while (iterator.hasNext()) {
      var argument = iterator.next();
      var nextIndex = iterator.nextIndex();
      var indent = nextIndex > dumpOffset || argument.startsWith("-") ? "" : "  ";
      consumer.accept(indent + argument);
      if (nextIndex > dumpLimit) {
        var last = arguments.size() - 1;
        var diff = last - nextIndex;
        if (diff > 1) {
          consumer.accept(indent + "... [omitted " + diff + " arguments]");
        }
        consumer.accept(indent + arguments.get(last));
        break;
      }
    }
    return this;
  }

  /** Set dump offset and limit. */
  Command mark(int limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("limit must be greater then zero: " + limit);
    }
    this.dumpOffset = arguments.size();
    this.dumpLimit = arguments.size() + limit;
    return this;
  }

  /** Set argument file support. */
  Command setExecutableSupportsArgumentFile(boolean executableSupportsArgumentFile) {
    this.executableSupportsArgumentFile = executableSupportsArgumentFile;
    return this;
  }

  /** Put the tool into the internal map of tools. */
  Command setToolProvider(ToolProvider tool) {
    if (tools == Collections.EMPTY_MAP) {
      tools = new TreeMap<>();
    }
    tools.put(tool.name(), tool);
    return this;
  }

  Command setTemporaryDirectory(Path temporaryDirectory) {
    this.temporaryDirectory = temporaryDirectory;
    return this;
  }

  Command setExecutableToProgramOperator(UnaryOperator<String> executableToProgramOperator) {
    this.executableToProgramOperator = executableToProgramOperator;
    return this;
  }

  /** Create new argument array based on this command's arguments. */
  String[] toArgumentsArray() {
    return arguments.toArray(new String[0]);
  }

  /** Create new {@link ProcessBuilder} instance based on this command setup. */
  ProcessBuilder toProcessBuilder() {
    List<String> strings = new ArrayList<>(1 + arguments.size());
    var program = executableToProgramOperator.apply(executable);
    strings.add(program);
    strings.addAll(arguments);
    var commandLineLength = String.join(" ", strings).length();
    if (commandLineLength > 32000) {
      if (!executableSupportsArgumentFile) {
        throw new IllegalStateException(
            String.format(
                "large command line (%s) detected, but %s does not support @argument file",
                commandLineLength, executable));
      }
      var timestamp = Instant.now().toString().replace("-", "").replace(":", "");
      var prefix = "bach-" + executable + "-arguments-" + timestamp + "-";
      try {
        var tempFile = Files.createTempFile(temporaryDirectory, prefix, ".txt");
        strings = List.of(program, "@" + Files.write(tempFile, arguments));
      } catch (IOException e) {
        throw new UncheckedIOException("creating temporary arguments file failed", e);
      }
    }
    var processBuilder = new ProcessBuilder(strings);
    processBuilder.redirectErrorStream(true);
    return processBuilder;
  }

  /** Run this command, returning zero for a successful run. */
  @Override
  public Integer apply(Bach bach) {
    return run(bach, UnaryOperator.identity(), this::toProcessBuilder);
  }

  /** Run this command. */
  void run(Bach bach) {
    var result = apply(bach);
    var successful = result == 0;
    if (successful) {
      return;
    }
    throw new Error("expected an exit code of zero, but got: " + result);
  }

  /** Run this command, returning zero for a successful run. */
  int run(Bach bach, UnaryOperator<ToolProvider> operator, Supplier<ProcessBuilder> supplier) {
    if (bach.log.debug()) {
      List<String> lines = new ArrayList<>();
      dump(lines::add);
      bach.log.debug("running %s with %s argument(s)", executable, arguments.size());
      bach.log.debug("%s", String.join("\n", lines));
    }
    var out = bach.var.streamOut;
    var err = bach.var.streamErr;
    var foundationTool = ToolProvider.findFirst(executable).orElse(null);
    var tool = tools.getOrDefault(executable, foundationTool);
    if (tool != null) {
      return operator.apply(tool).run(out, err, toArgumentsArray());
    }
    var processBuilder = supplier.get();
    if (bach.log.debug()) {
      var program = processBuilder.command().get(0);
      if (!executable.equals(program)) {
        bach.log.debug("replaced executable `%s` with program `%s`", executable, program);
      }
    }
    try {
      var process = processBuilder.start();
      process.getInputStream().transferTo(out);
      return process.waitFor();
    } catch (IOException | InterruptedException e) {
      throw new Error("executing `" + executable + "` failed", e);
    }
  }
}

/** Directory tree layout. */
enum Layout {
  /** Unknown layout. */
  UNKNOWN,

  /**
   * Module descriptor resides in folder with same name as the module.
   *
   * <p>Pattern: {@code <root>/<module name>/module-info.java}
   *
   * <ul>
   *   <li>{@code <src/> tool/module-info.java} containing {@code module tool {}}
   *   <li>{@code <src/main/> tool/module-info.java} containing {@code module tool {}}
   *   <li>{@code <src/test/> tool/module-info.java} containing {@code open module tool {}} or a
   *       {@code module-info.test} configuration file.
   * </ul>
   */
  BASIC,

  /**
   * Module group folder first and no module name but "java" in the directory hierarchy.
   *
   * <p>Pattern: {@code <root>/[main|test|...]/java/module-info.java}
   *
   * <ul>
   *   <li>{@code src/main/java/ module-info.java} containing {@code module tool {}}
   *   <li>{@code src/test/java/ module-info.java} containing {@code open module tool {}} or a
   *       {@code module-info.test} configuration file.
   * </ul>
   */
  MAVEN;

  static Layout of(Path root) {
    if (Files.notExists(root)) {
      // throw new IllegalArgumentException("root path must exist: " + root);
      return UNKNOWN;
    }
    if (!Files.isDirectory(root)) {
      // throw new IllegalArgumentException("root path must be a directory: " + root);
      return UNKNOWN;
    }
    try {
      var none = Path.of("");
      var path =
          Files.find(root, 10, (p, a) -> p.endsWith("module-info.java"))
              .map(root::relativize)
              .findFirst()
              .orElse(none);
      if (path == none) {
        // throw new Error("no module descriptor found in " + root)
        return UNKNOWN;
      }
      var name = ModuleInfo.of(root.resolve(path)).name;
      if (path.getNameCount() == 2) {
        if (!path.startsWith(name)) {
          throw new Error(String.format("expected path to start with '%s': %s", name, path));
        }
        return BASIC;
      }
      if (path.getNameCount() == 3) {
        if (!path.getParent().endsWith("java")) {
          var message = String.format("expected module-info.java in 'java' directory: %s", path);
          throw new Error(message);
        }
        return MAVEN;
      }
      return UNKNOWN;
    } catch (Exception e) {
      throw new Error("detection failed " + e, e);
    }
  }
}

/** Simple module information collector. */
class ModuleInfo {

  private static final Pattern NAME = Pattern.compile("(module)\\s+(.+)\\s*\\{.*");

  private static final Pattern REQUIRES = Pattern.compile("requires (.+?);", Pattern.DOTALL);

  static ModuleInfo of(Path path) {
    if (Files.isDirectory(path)) {
      path = path.resolve("module-info.java");
    }
    try {
      return of(Files.readString(path));
    } catch (IOException e) {
      throw new UncheckedIOException("reading '" + path + "' failed", e);
    }
  }

  static ModuleInfo of(List<String> lines) {
    return of(String.join("\n", lines));
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
      } catch (IOException e) {
        throw new UncheckedIOException("walking path failed for: " + root, e);
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

  final String name;
  final Set<String> requires;

  private ModuleInfo(String name, Set<String> requires) {
    this.name = name;
    this.requires = Set.copyOf(requires);
  }
}

/** Available properties and their default values. */
enum Property {
  /** Action to call when no explicit actions are supplied. */
  ACTION(Action.BUILD.name()),

  /** Exit code value returned by {@link Action#FAIL}. */
  FAIL_CODE("4711"),

  /** Return on first non-zero operation. */
  FAIL_FAST("true"),

  /** Log format string used by {@link Bach.Log#log(Level, String)}. */
  LOG_FORMAT("[%s] %s%n"),

  /** Initial log level threshold. */
  LOG_LEVEL("ALL"),

  /** Use only locally cached assets. */
  OFFLINE("false"),

  /** Root path for all source realms. */
  PATH_SOURCE("src"),

  /** Root path for all generated assets. */
  PATH_TARGET("target/bach"),

  /** Cache of binary tools. */
  PATH_CACHE_TOOLS(".bach/tools"),

  /** Cache of resolved modules. */
  PATH_CACHE_MODULES(".bach/modules"),

  /** Name of the project. */
  PROJECT_NAME("project"),

  /** Version of the project. */
  PROJECT_VERSION("1.0.0-SNAPSHOT"),

  /** Main entry-point of the project. */
  PROJECT_LAUNCH("n/a"),

  /** Names of source set realms. */
  PROJECT_REALMS("main, test"),

  /** Name of test realm. */
  PROJECT_REALM_TEST("test"),

  /** Gradle URI. */
  TOOL_GRADLE_URI("https://services.gradle.org/distributions/gradle-5.2-bin.zip"),

  /** JUnit Platform Console Standalone URI. */
  TOOL_JUNIT_URI(
      "http://central.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.4.0/junit-platform-console-standalone-1.4.0.jar"),

  /** Maven URI. */
  TOOL_MAVEN_URI(
      "https://archive.apache.org/dist/maven/maven-3/3.6.0/binaries/apache-maven-3.6.0-bin.zip");

  final String key;
  final String defaultValue;
  final String description;

  Property(String defaultValue, String... description) {
    this.key = "bach." + name().toLowerCase().replace('_', '.');
    this.defaultValue = defaultValue;
    this.description = String.join("", description);
  }
}

/** Tiny static helpers. */
class Util {

  /** Get integer value of the supplied string. */
  static int integer(String string, Integer defaultValue) {
    try {
      return Integer.valueOf(string);
    } catch (NumberFormatException e) {
      if (defaultValue == null) {
        throw e;
      }
      return defaultValue;
    }
  }

  /** Get last path or the root of the supplied path. */
  static Path last(Path path) {
    var last = path.getFileName();
    return last == null ? path.getRoot() : last;
  }

  /** Load {@code .properties} from supplied path. */
  static Properties loadProperties(Path path) {
    var properties = new Properties();
    if (Files.exists(path)) {
      try (var stream = Files.newInputStream(path)) {
        properties.load(stream);
      } catch (IOException e) {
        throw new UncheckedIOException("loading properties failed", e);
      }
    }
    return properties;
  }

  /** Test supplied path for pointing to a Java source unit file. */
  static boolean isJavaFile(Path path) {
    if (Files.isRegularFile(path)) {
      var name = path.getFileName().toString();
      if (name.endsWith(".java")) {
        return name.indexOf('.') == name.length() - 5; // single dot in filename
      }
    }
    return false;
  }

  /** Test for an empty directory */
  static boolean isEmpty(Path directory) {
    try (var stream = Files.newDirectoryStream(directory)) {
      return !stream.iterator().hasNext();
    } catch (IOException e) {
      throw new UncheckedIOException("streaming contents failed for: " + directory, e);
    }
  }

  /** Get path pointing to the current Java home directory. */
  static Path currentJavaHome() {
    var executable = ProcessHandle.current().info().command().map(Path::of).orElseThrow();
    return executable.getParent().getParent().toAbsolutePath();
  }

  /** Find foundation JDK command by its name. */
  static Optional<Path> findJdkCommandPath(String name) {
    // Path.of(System.getProperty("java.home")).toAbsolutePath().normalize()
    var home = currentJavaHome();
    var bin = home.resolve("bin");
    for (var suffix : List.of("", ".exe")) {
      var tool = bin.resolve(name + suffix);
      if (Files.isExecutable(tool)) {
        return Optional.of(tool);
      }
    }
    return Optional.empty();
  }

  /** Find foundation JDK command by its name. */
  static String getJdkCommand(String name) {
    return findJdkCommandPath(name).map(Object::toString).orElseThrow();
  }

  /** Extract path last element from the supplied uri. */
  static String fileName(URI uri) {
    var urlString = uri.getPath();
    var begin = urlString.lastIndexOf('/') + 1;
    return urlString.substring(begin).split("\\?")[0].split("#")[0];
  }

  /** Delete directory. */
  static void removeTree(Path root) {
    removeTree(root, path -> true);
  }

  /** Delete selected files and directories from the root directory. */
  static void removeTree(Path root, Predicate<Path> filter) {
    // trivial case: delete existing single file or empty directory right away
    try {
      if (Files.deleteIfExists(root)) {
        return;
      }
    } catch (IOException ignored) {
      // fall-through
    }
    // default case: walk the tree...
    try (var stream = Files.walk(root)) {
      var selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
      for (var path : selected.collect(Collectors.toList())) {
        Files.deleteIfExists(path);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("removing tree failed: " + root, e);
    }
  }

  /** Mark the supplied file as executable. */
  static Path setExecutable(Path path) {
    if (Files.isExecutable(path)) {
      return path;
    }
    if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
      return path;
    }
    var program = path.toFile();
    if (!program.setExecutable(true)) {
      throw new IllegalStateException("can't set executable flag: " + program);
    }
    return path;
  }

  /** Return list of child directories directly present in {@code root} path. */
  static List<Path> findDirectories(Path root) {
    if (Files.notExists(root)) {
      return Collections.emptyList();
    }
    try (var paths = Files.find(root, 1, (path, attr) -> Files.isDirectory(path))) {
      return paths.filter(path -> !root.equals(path)).collect(Collectors.toList());
    } catch (IOException e) {
      throw new UncheckedIOException("findDirectories failed for root: " + root, e);
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

  static URI maven(String group, String artifact, String version) {
    return maven(group, artifact, version, "");
  }

  static URI maven(String group, String artifact, String version, String classifier) {
    if (!classifier.isEmpty() && !classifier.startsWith("-")) {
      classifier = "-" + classifier;
    }
    var repo = "https://repo1.maven.org/maven2";
    var file = artifact + "-" + version + classifier + ".jar";
    return URI.create(String.join("/", repo, group.replace('.', '/'), artifact, version, file));
  }
}

/** External tool. */
interface Tool extends Function<Bach, Integer> {

  static Tool of(String name, String... arguments) {
    return of(name, List.of(arguments));
  }

  static Tool of(String name, List<?> arguments) {
    switch (name.toLowerCase()) {
      case "gradle":
        return new Tool.Gradle(arguments);
      case "junit":
        return new Tool.JUnit(arguments);
      case "maven":
      case "mvn":
        return new Tool.Maven(arguments);
    }
    throw new UnsupportedOperationException("tool not supported: " + name);
  }

  default Object run(Bach bach) {
    var code = bach.run(this);
    if (code != 0) {
      throw new Error(getClass().getSimpleName() + " failed with code " + code);
    }
    return code;
  }

  /** Load an asset from the supplied URI to the specified target directory. */
  class Download implements Tool {

    final URI uri;
    final Path directory;
    final String fileName;
    final Path target;

    Download(URI uri, Path directory) {
      this(uri, directory, Util.fileName(uri));
    }

    Download(URI uri, Path directory, String fileName) {
      this.uri = uri;
      this.directory = directory;
      this.fileName = fileName;
      this.target = directory.resolve(fileName);
    }

    @Override
    public Integer apply(Bach bach) {
      var log = bach.log;
      log.debug("download(uri:%s, directory:%s, fileName:%s)", uri, directory, fileName);
      if (bach.var.offline) {
        if (Files.exists(target)) {
          return 0;
        }
        throw new Error("offline mode is active -- missing file " + target);
      }
      try {
        Files.createDirectories(directory);
        var connection = uri.toURL().openConnection();
        try (var sourceStream = connection.getInputStream()) {
          var urlLastModifiedMillis = connection.getLastModified();
          var urlLastModifiedTime = FileTime.fromMillis(urlLastModifiedMillis);
          if (Files.exists(target)) {
            log.debug("local file already exists -- comparing properties to remote file...");
            var unknownTime = urlLastModifiedMillis == 0L;
            if (Files.getLastModifiedTime(target).equals(urlLastModifiedTime) || unknownTime) {
              var localFileSize = Files.size(target);
              var contentLength = connection.getContentLengthLong();
              if (localFileSize == contentLength) {
                log.debug("local and remote file properties seem to match, using `%s`", target);
                return 0;
              }
            }
            log.debug("local file `%s` differs from remote one -- replacing it", target);
          }
          log.debug("transferring `%s`...", uri);
          try (var targetStream = Files.newOutputStream(target)) {
            sourceStream.transferTo(targetStream);
          }
          if (urlLastModifiedMillis != 0L) {
            Files.setLastModifiedTime(target, urlLastModifiedTime);
          }
          log.info("`%s` downloaded [%s|%s]", fileName, Files.size(target), urlLastModifiedTime);
        }
      } catch (Exception e) {
        log.log(Level.ERROR, "download failed: " + e);
        return 1;
      }
      return 0;
    }

    @Override
    public Path run(Bach bach) {
      bach.run(this);
      return this.target;
    }
  }

  /** Unzip. */
  class Extract implements Tool {

    final Path zip;
    Path target;

    Extract(Path zip) {
      this.zip = zip;
    }

    @Override
    public Integer apply(Bach bach) {
      try {
        var jar = ToolProvider.findFirst("jar").orElseThrow();
        var listing = new StringWriter();
        var printWriter = new PrintWriter(listing);
        jar.run(printWriter, printWriter, "--list", "--file", zip.toString());
        // TODO Find better way to extract root folder name...
        var root = Path.of(listing.toString().split("\\R")[0]);
        var home = bach.based(Property.PATH_CACHE_TOOLS).resolve(root);
        if (Files.notExists(home)) {
          jar.run(System.out, System.err, "--extract", "--file", zip.toString());
          Files.move(root, home);
        }
        // done
        target = home.normalize().toAbsolutePath();
        return 0;
      } catch (IOException e) {
        bach.log.log(Level.ERROR, "Extraction of " + zip + " failed: " + e.getMessage());
        return 1;
      }
    }

    @Override
    public Path run(Bach bach) {
      Tool.super.run(bach);
      return target;
    }
  }

  /** Google Java Format. */
  class GoogleJavaFormat implements Tool {

    final String version;
    final boolean replace;
    final List<Path> roots;

    GoogleJavaFormat(boolean replace, List<Path> roots) {
      this("1.7", replace, roots);
    }

    GoogleJavaFormat(String version, boolean replace, List<Path> roots) {
      this.version = version;
      this.replace = replace;
      this.roots = roots;
    }

    @Override
    public Integer apply(Bach bach) {
      var base = "https://github.com/google/";
      var name = "google-java-format";
      var file = name + "-" + version + "-all-deps.jar";
      var uri = URI.create(base + name + "/releases/download/" + name + "-" + version + "/" + file);
      var jar = new Download(uri, bach.based(Property.PATH_CACHE_TOOLS).resolve(name)).run(bach);

      var command = new Command("java");
      command.add("-jar");
      command.add(jar);
      if (replace) {
        command.add("--replace");
      } else {
        command.add("--dry-run");
        command.add("--set-exit-if-changed");
      }
      command.mark(10);
      command.addAllJavaFiles(roots);
      return bach.run("GoogleJavaFormat", command);
    }
  }

  /** Gradle. */
  class Gradle implements Tool {

    final List<?> arguments;

    Gradle(List<?> arguments) {
      this.arguments = arguments;
    }

    @Override
    public Integer apply(Bach bach) {
      // download
      var uri = URI.create(bach.get(Property.TOOL_GRADLE_URI));
      var zip = new Download(uri, bach.based(Property.PATH_CACHE_TOOLS)).run(bach);
      // extract
      var home = new Extract(zip).run(bach);
      // run
      var win = System.getProperty("os.name").toLowerCase().contains("win");
      var name = "gradle" + (win ? ".bat" : "");
      var executable = Util.setExecutable(home.resolve("bin").resolve(name));
      var command = new Command(executable.toString()).addAll(arguments);
      return command.apply(bach);
    }
  }

  /** JUnit Platform Console Standalone. */
  class JUnit implements Tool {

    static Path install(Bach bach) {
      var art = "junit-platform-console-standalone";
      var uri = URI.create(bach.get(Property.TOOL_JUNIT_URI));
      return new Download(uri, bach.based(Property.PATH_CACHE_TOOLS).resolve(art)).run(bach);
    }

    final List<?> arguments;

    JUnit(List<?> arguments) {
      this.arguments = arguments;
    }

    @Override
    public Integer apply(Bach bach) {
      var jar = install(bach);
      var java = new Command("java");
      java.add("-ea");
      java.add("-jar").add(jar);
      java.addAll(arguments);
      return java.apply(bach);
    }
  }

  /** Maven. */
  class Maven implements Tool {

    final List<?> arguments;

    Maven(List<?> arguments) {
      this.arguments = arguments;
    }

    @Override
    public Integer apply(Bach bach) {
      // download
      var uri = URI.create(bach.get(Property.TOOL_MAVEN_URI));
      var zip = new Download(uri, bach.based(Property.PATH_CACHE_TOOLS)).run(bach);
      // extract
      var home = new Extract(zip).run(bach);
      // run
      var win = System.getProperty("os.name").toLowerCase().contains("win");
      var name = "mvn" + (win ? ".cmd" : "");
      var executable = Util.setExecutable(home.resolve("bin").resolve(name));
      var command = new Command(executable.toString()).addAll(arguments);
      return command.apply(bach);
    }
  }
}
