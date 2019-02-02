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
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Java Shell Builder. */
class Bach {

  /** Main entry-point. */
  static void main(String... args) {
    var bach = new Bach(args);
    var code = bach.run();
    if (code != 0) {
      throw new Error("Bach finished with exit code " + code);
    }
  }

  final List<String> arguments;
  final Log log = new Log();
  final Var var = new Var();

  Bach(String... arguments) {
    this.arguments = List.of(arguments);
  }

  /** Main entry-point entry-point. */
  int run() {
    // Welcome!
    log.info("Bach");

    // Debug...
    if (arguments.isEmpty()) {
      log.debug("No arguments");
    } else {
      log.debug("Arguments");
      for (var argument : arguments) {
        log.debug("  -> %s", argument);
      }
    }
    log.debug("Properties");
    for (var property : Property.values()) {
      log.debug("  %s -> %s", property.key, property.get());
    }

    // Action!
    if (arguments.isEmpty()) {
      var action = Action.valueOf(Property.ACTION.get().toUpperCase());
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

  /** Logging support. */
  class Log {
    /** Logger function defaults to {@linkplain #log(Level, String)} of this class. */
    BiConsumer<Level, String> logger = this::log;

    /** Logger format in {@link java.util.Formatter} style. */
    String format = Property.LOG_FORMAT.get();

    /** Current logger level threshold. */
    Level level = Level.valueOf(Property.LOG_LEVEL.get());

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
    boolean failFast = Util.isTrue(Property.FAIL_FAST);

    /** Use only locally cached assets. */
    boolean offline = Util.isTrue(Property.OFFLINE);

    /** Print stream to emit error messages to. */
    PrintStream streamErr = System.err;

    /** Print stream to emit standard messages to. */
    PrintStream streamOut = System.out;
  }
}

/** Main program actions. */
enum Action implements Function<Bach, Integer> {
  BUILD {
    @Override
    public Integer apply(Bach bach) {
      bach.log.log(Level.WARNING, name() + " isn't implemented, yet");
      return 0;
    }

    @Override
    public String toString() {
      return "Build project '" + Util.last(Property.BASE) + "' in current directory";
    }
  },

  CLEAN {
    @Override
    public Integer apply(Bach bach) {
      bach.log.log(Level.WARNING, name() + " isn't implemented, yet");
      return 0;
    }

    @Override
    public String toString() {
      return "Delete directory " + Property.PATH_TARGET.get() + " - but keep caches intact.";
    }
  },

  ERASE {
    @Override
    public Integer apply(Bach bach) {
      bach.log.log(Level.WARNING, name() + " isn't implemented, yet");
      return 0;
    }

    @Override
    public String toString() {
      return "Delete out all generated assets, including caches.";
    }
  },

  FAIL {
    @Override
    public Integer apply(Bach bach) {
      var code = Util.integer(Property.FAIL_CODE.get(), -1);
      bach.log.log(Level.WARNING, "Setting exit code to " + code);
      return code;
    }

    @Override
    public String toString() {
      return "Set exit code to an non-zero value to fail the run.";
    }
  },

  HELP {
    @Override
    public Integer apply(Bach bach) {
      var out = bach.var.streamOut;
      out.println();
      for (var action : Action.values()) {
        out.println(String.format(" %-9s -> %s", action.name().toLowerCase(), action));
      }
      out.println();
      return 0;
    }

    @Override
    public String toString() {
      return "Display help screen ... F1, F1, F1!";
    }
  },

  SCAFFOLD {
    @Override
    public Integer apply(Bach bach) {
      bach.log.log(Level.WARNING, name() + " isn't implemented, yet");
      return 0;
    }

    @Override
    public String toString() {
      return "Create starter project '" + Util.last(Property.BASE) + "' in current directory";
    }
  },

  TOOL {
    @Override
    public Integer apply(Bach bach) {
      var args = new LinkedList<>(bach.arguments);
      if (args.size() < 2) {
        bach.log.log(Level.WARNING, "Too few arguments for executing an external tool: " + args);
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

    @Override
    public String toString() {
      return "Execute an external tool consuming all remaining arguments.";
    }
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
    roots.forEach(root -> addAll(root, predicate));
    return this;
  }

  /** Add all .java source files by walking specified root paths recursively. */
  Command addAllJavaFiles(List<Path> roots) {
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

  /** Root path for all generated assets. */
  PATH_TARGET("target/bach"),

  /** Cache of binary tools. */
  PATH_CACHE_TOOLS(".bach/tools"),

  /** Gradle URI. */
  TOOL_GRADLE_URI("https://services.gradle.org/distributions/gradle-5.1.1-bin.zip"),

  /** JUnit Platform Console Standalone URI. */
  TOOL_JUNIT_URI(
      "http://central.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.4.0-RC2/junit-platform-console-standalone-1.4.0-RC2.jar"),

  /** Maven URI. */
  TOOL_MAVEN_URI(
      "https://archive.apache.org/dist/maven/maven-3/3.6.0/binaries/apache-maven-3.6.0-bin.zip");

  /** Base directory of the current project. */
  static final Path BASE = Path.of(System.getProperty("bach.base", System.getProperty("user.dir")));

  /** Properties loaded from {@code ${BASE}/bach.properties} file. */
  static final Map<String, String> PROPERTIES = load(BASE.resolve("bach.properties"));

  static Map<String, String> load(Path path) {
    if (Files.notExists(path)) {
      return Map.of();
    }
    try (var lines = Files.lines(path)) {
      return load(lines);
    } catch (IOException e) {
      throw new UncheckedIOException("loading properties failed", e);
    }
  }

  static Map<String, String> load(Stream<String> stream) {
    var lines =
        stream
            .map(String::stripLeading)
            .filter(s -> !s.startsWith("#"))
            .filter(s -> s.indexOf('=') > 0)
            .collect(Collectors.toList());
    if (lines.isEmpty()) {
      return Map.of();
    }
    var map = new HashMap<String, String>();
    for (var line : lines) {
      var key = line.substring(0, line.indexOf('='));
      var value = line.substring(key.length() + 1);
      map.put(key.strip(), value.strip());
    }
    return Map.copyOf(map);
  }

  final String key;
  final String defaultValue;
  final String description;

  Property(String defaultValue, String... description) {
    this.key = "bach." + name().toLowerCase().replace('_', '.');
    this.defaultValue = defaultValue;
    this.description = String.join("", description);
  }

  /** Get configured string value of this property. */
  String get() {
    return System.getProperty(key, PROPERTIES.getOrDefault(key, defaultValue));
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

  /** Get path value of the supplied property. */
  static Path path(Property property) {
    return Path.of(property.get());
  }

  /** Get boolean value of the supplied property. */
  static boolean isTrue(Property property) {
    return Boolean.valueOf(property.get());
  }

  /** Get last path or the root of the supplied path. */
  static Path last(Path path) {
    var last = path.getFileName();
    return last == null ? path.getRoot() : last;
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
        var home = Util.path(Property.PATH_CACHE_TOOLS).resolve(root);
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
      var jar = new Download(uri, Util.path(Property.PATH_CACHE_TOOLS).resolve(name)).run(bach);

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
      var uri = URI.create(Property.TOOL_GRADLE_URI.get());
      var zip = new Download(uri, Util.path(Property.PATH_CACHE_TOOLS)).run(bach);
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
      var uri = URI.create(Property.TOOL_JUNIT_URI.get());
      var jar = new Download(uri, Util.path(Property.PATH_CACHE_TOOLS).resolve(art));
      return jar.run(bach);
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
      var uri = URI.create(Property.TOOL_MAVEN_URI.get());
      var zip = new Download(uri, Util.path(Property.PATH_CACHE_TOOLS)).run(bach);
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
