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
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
  public static void main(String... args) {
    var bach = new Bach();
    var code = bach.main(List.of(args));
    if (code != 0) {
      throw new AssertionError("Bach finished with exit code " + code);
    }
  }

  final Log log = new Log();
  final Var var = new Var();

  /** Main entry-point. */
  int main(List<String> args) {
    log.info("Bach");
    log.debug("args = " + args);
    log.debug("Properties");
    for (var property : Property.values()) {
      log.debug("%s -> %s", property.key, Util.get(property));
    }
    return args.contains("ERROR") ? 1 : 0;
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
    String format = Util.get(Property.LOG_FORMAT);

    /** Current logger level threshold. */
    Level level = Level.valueOf(Util.get(Property.LOG_LEVEL));

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
    /** Use only locally cached assets. */
    boolean offline = Util.isTrue(Property.OFFLINE);

    /** Print stream to emit error messages to. */
    PrintStream streamErr = System.err;

    /** Print stream to emit standard messages to. */
    PrintStream streamOut = System.out;
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

  /**
   * Run this command.
   *
   * @throws AssertionError if the execution result is not zero
   */
  void run(Bach bach) {
    var result = apply(bach);
    var successful = result == 0;
    if (successful) {
      return;
    }
    throw new AssertionError("expected an exit code of zero, but got: " + result);
  }

  /**
   * Run this command, returning zero for a successful run.
   *
   * @return the result of executing the tool. A return value of 0 means the tool did not encounter
   *     any errors; any other value indicates that at least one error occurred during execution.
   */
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

/** Default constants. */
enum Property {
  LOG_FORMAT("[%s] %s%n"),

  LOG_LEVEL("ALL"),

  OFFLINE("false"),

  PATH_TOOLS(".bach/tools");

  static final Path BASE = Path.of(System.getProperty("user.dir"));

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
}

/** Tiny static helpers. */
class Util {

  /** Get string value of the supplied property. */
  static String get(Property property) {
    var key = property.key;
    return System.getProperty(key, Property.PROPERTIES.getOrDefault(key, property.defaultValue));
  }

  /** Get path value of the supplied property. */
  static Path path(Property property) {
    return Path.of(get(property));
  }

  /** Get boolean value of the supplied property. */
  static boolean isTrue(Property property) {
    return Boolean.valueOf(get(property));
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
}

/** External tool. */
interface Tool extends Function<Bach, Integer> {

  default Object run(Bach bach) {
    return bach.run(this);
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
      var jar = new Download(uri, Util.path(Property.PATH_TOOLS).resolve(name)).run(bach);

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
}
