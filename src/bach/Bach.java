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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

class Bach implements Function<String[], Integer> {

  private static final String VERSION = "2.0";

  public static void main(String... args) {
    System.exit(new Bach().apply(args));
  }

  final Variables vars;
  final Logging log;
  final Utilities util;

  Bach() {
    this.vars = new Variables();
    this.log = new Logging();
    this.util = new Utilities();

    try {
      Files.createDirectories(vars.temporary);
    } catch (IOException e) {
      throw new UncheckedIOException("creating directories failed: " + vars.temporary, e);
    }
  }

  @Override
  public Integer apply(String... args) {
    var start = Instant.now();
    log.info("Bach {0}", VERSION);

    var arguments = List.of(args);
    try {
      if (arguments.isEmpty()) {
        build();
      } else {
        log.debug("Arguments: {0}", arguments);
        for (var arg : args) {
          Bach.class.getMethod(arg).invoke(this);
        }
      }
      log.info("SUCCESS");
      return 0;
    } catch (Throwable throwable) {
      log.log(System.Logger.Level.ERROR, "Bach failed to execute: " + throwable.getMessage());
      return 1;
    } finally {
      var duration = Duration.between(start, Instant.now());
      log.info("Bach execution took {0} milliseconds.", duration.toMillis());
    }
  }

  public void build() {
    log.info("Building...");
    format();
  }

  public void clean() {
    log.info("Cleaning up...");
  }

  public void format() {
    format(Boolean.getBoolean("bach.format.replace"), ".");
  }

  void format(boolean replace, String... directories) {
    log.info("Formatting {0}...", List.of(directories));
    var version = "1.7";
    var base = "https://github.com/google/";
    var name = "google-java-format";
    var file = name + "-" + version + "-all-deps.jar";
    var uri = URI.create(base + name + "/releases/download/" + name + "-" + version + "/" + file);
    var jar = util.download(uri, vars.tools.resolve(name));

    var command = new Command(util.currentJavaHome().resolve("bin").resolve("java").toString());
    command.add("-jar");
    command.add(jar);
    if (replace) {
      command.add("--replace");
    } else {
      command.add("--dry-run");
      command.add("--set-exit-if-changed");
    }
    command.addAllJavaFiles(Arrays.stream(directories).map(Path::of).collect(Collectors.toList()));
    var code = run(name, command);
    if (code != 0) {
      throw new Error("format failed");
    }
  }

  /** Create command for executable name and any arguments. */
  Command command(String executable, Object... arguments) {
    return new Command(executable).addAll(arguments);
  }

  /** Run named executable with given arguments. */
  int run(String executable, Object... arguments) {
    log.info("[run] {0} {1}", executable, List.of(arguments));
    return command(executable, arguments).get();
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
    log.info("[run] {0}...", caption);
    var results = stream.map(CompletableFuture::supplyAsync).map(CompletableFuture::join);
    var result = results.reduce(0, Integer::sum);
    log.info("[run] {0} done.", caption);
    if (result != 0) {
      throw new Error(caption + " finished with exit code " + result);
    }
    return result;
  }

  /** Command line program and in-process tool abstraction. */
  class Command implements Supplier<Integer> {

    final String executable;
    final List<String> arguments = new ArrayList<>();
    private int dumpLimit = Integer.MAX_VALUE;
    private int dumpOffset = Integer.MAX_VALUE;

    private Map<String, ToolProvider> tools = Collections.emptyMap();
    private boolean executableSupportsArgumentFile = false;
    private UnaryOperator<String> executableToProgramOperator = UnaryOperator.identity();
    private Path temporaryDirectory = Bach.this.vars.temporary;

    /** Initialize this command instance. */
    Command(String executable) {
      this.executable = executable;
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
      return addAll(roots, util::isJavaFile);
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
        if (executableSupportsArgumentFile) {
          var timestamp = Instant.now().toString().replace("-", "").replace(":", "");
          var prefix = "bach-" + executable + "-arguments-" + timestamp + "-";
          try {
            var tempFile = Files.createTempFile(temporaryDirectory, prefix, ".txt");
            strings = List.of(program, "@" + Files.write(tempFile, arguments));
          } catch (IOException e) {
            throw new UncheckedIOException("creating temporary arguments file failed", e);
          }
        } else {
          log.info(
              "large command line ({0}) detected, but {1} does not support @argument file",
              commandLineLength, executable);
        }
      }
      var processBuilder = new ProcessBuilder(strings);
      processBuilder.redirectErrorStream(true);
      return processBuilder;
    }

    /**
     * Run this command, returning zero for a successful run.
     *
     * @return the result of executing the tool. A return value of 0 means the tool did not
     *     encounter any errors; any other value indicates that at least one error occurred during
     *     execution.
     */
    @Override
    public Integer get() {
      return run(UnaryOperator.identity(), this::toProcessBuilder);
    }

    /**
     * Run this command.
     *
     * @throws AssertionError if the execution result is not zero
     */
    void run() {
      var result = get();
      var successful = result == 0;
      if (successful) {
        return;
      }
      throw new AssertionError("expected an exit code of zero, but got: " + result);
    }

    /**
     * Run this command, returning zero for a successful run.
     *
     * @return the result of executing the tool. A return value of 0 means the tool did not
     *     encounter any errors; any other value indicates that at least one error occurred during
     *     execution.
     */
    int run(UnaryOperator<ToolProvider> operator, Supplier<ProcessBuilder> supplier) {
      if (log.debug()) {
        List<String> lines = new ArrayList<>();
        dump(lines::add);
        log.debug("running {0} with {1} argument(s)", executable, arguments.size());
        log.debug("{0}", String.join("\n", lines));
      }
      var out = log.printStreamOut;
      var err = log.printStreamErr;
      var foundationTool = ToolProvider.findFirst(executable).orElse(null);
      var tool = tools.getOrDefault(executable, foundationTool);
      if (tool != null) {
        return operator.apply(tool).run(out, err, toArgumentsArray());
      }
      var processBuilder = supplier.get();
      if (log.debug()) {
        var program = processBuilder.command().get(0);
        if (!executable.equals(program)) {
          log.debug("replaced executable {0} with program {1}", executable, program);
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

  class Logging {

    PrintStream printStreamOut = System.out;

    PrintStream printStreamErr = System.err;

    /** Logger function. */
    BiConsumer<System.Logger.Level, String> consumer = this::log;

    /** Return {@code true} if debugging is enabled. */
    boolean debug() {
      return is(System.Logger.Level.DEBUG);
    }

    /** Return {@code true} if the supplied level is enabled. */
    boolean is(System.Logger.Level level) {
      return vars.logLevel.getSeverity() <= level.getSeverity();
    }

    /** Log debug level message. */
    void debug(String format, Object... arguments) {
      log(System.Logger.Level.DEBUG, format, arguments);
    }

    /** Log info level message. */
    void info(String format, Object... arguments) {
      log(System.Logger.Level.INFO, format, arguments);
    }

    void log(System.Logger.Level level, String format, Object... arguments) {
      consumer.accept(level, MessageFormat.format(format, arguments));
    }

    void log(System.Logger.Level level, String text) {
      var severity = level.getSeverity();
      if (severity >= vars.logLevel.getSeverity()) {
        var error = severity >= System.Logger.Level.ERROR.getSeverity();
        var stream = error ? printStreamErr : printStreamOut;
        stream.printf(vars.logFormat, level, text);
      }
    }
  }

  class Utilities {

    boolean isJavaFile(Path path) {
      if (Files.isRegularFile(path)) {
        var name = path.getFileName().toString();
        if (name.endsWith(".java")) {
          return name.indexOf('.') == name.length() - 5; // single dot in filename
        }
      }
      return false;
    }

    Path currentJavaHome() {
      var executable = ProcessHandle.current().info().command().map(Path::of).orElseThrow();
      return executable.getParent().getParent().toAbsolutePath();
    }

    String fileName(URI uri) {
      var urlString = uri.getPath();
      var begin = urlString.lastIndexOf('/') + 1;
      return urlString.substring(begin).split("\\?")[0].split("#")[0];
    }

    Path download(URI uri, Path targetDirectory) {
      return download(uri, targetDirectory, fileName(uri));
    }

    Path download(URI uri, Path targetDirectory, String fileName) {
      var targetPath = targetDirectory.resolve(fileName);
      if (Files.exists(targetPath)) {
        log.debug(
            "Using cached {0} in {1} instead of downloading it from {2}",
            fileName, targetDirectory, uri);
        return targetPath;
      }
      if (vars.offline) {
        throw new Error("offline mode is active -- missing file " + targetPath);
      }
      log.info("Downloading {0} to {1}...", uri, targetPath);
      try {
        var readableByteChannel = Channels.newChannel(uri.toURL().openStream());
        Files.createDirectories(targetDirectory);
        var fileOutputStream = new FileOutputStream(targetPath.toFile());
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        return targetPath;
      } catch (IOException e) {
        throw new UncheckedIOException("download failed", e);
      }
    }

    /** Delete directory. */
    void removeTree(Path root) {
      removeTree(root, path -> true);
    }

    /** Delete selected files and directories from the root directory. */
    void removeTree(Path root, Predicate<Path> filter) {
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

  class Variables {

    /** Log format in printf-style. */
    String logFormat = get("bach.log.format", "[%s] %s%n");

    /** Log level. */
    System.Logger.Level logLevel = System.Logger.Level.valueOf(get("bach.log.level", "ALL"));

    /** Offline mode switch. */
    boolean offline = Boolean.getBoolean("bach.offline");

    /** Temporary path. */
    Path temporary = get("bach.path.temporary", Path.of(get("java.io.tmpdir"), ".bach"));

    /** Tools cache path. */
    Path tools = Path.of(get("bach.path.tools", ".bach/tools"));

    private String get(String key) {
      return System.getProperty(key);
    }

    private String get(String key, String defaultValue) {
      return System.getProperty(key, defaultValue);
    }

    private Path get(String key, Path defaultPath) {
      return Path.of(get(key, defaultPath.toString()));
    }
  }
}
