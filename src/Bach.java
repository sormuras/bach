import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.StackTrace;

public record Bach(
    Options options, Logbook logbook, Paths paths, Externals externals, Tools tools) {

  public static void main(String... args) {
    var bach = Bach.of(args);
    var code = bach.main();
    System.exit(code);
  }

  public static Bach of(String... args) {
    return Bach.of(System.out::println, System.err::println, args);
  }

  public static Bach of(Consumer<String> out, Consumer<String> err, String... args) {
    var options = Options.of(args);
    var properties = PathSupport.properties(options.__chroot.resolve("bach.properties"));
    var programs = new TreeMap<Path, URI>();
    for (var key : properties.stringPropertyNames()) {
      if (key.startsWith(".bach/external-tool-program/")) {
        var to = Path.of(key).normalize();
        var from = URI.create(properties.getProperty(key));
        programs.put(to, from);
      }
    }
    return new Bach(
        options,
        new Logbook(out, err, options.__logbook_threshold, new ConcurrentLinkedDeque<>()),
        new Paths(options.__chroot, options.__destination),
        new Externals(programs),
        new Tools(
            ToolFinder.compose(
                ToolFinder.ofProperties(options.__chroot.resolve(".bach/tool-provider")),
                ToolFinder.ofPrograms(
                    options.__chroot.resolve(".bach/external-tool-program"),
                    Path.of(System.getProperty("java.home"), "bin", "java"),
                    "java.args"),
                ToolFinder.of(
                    new ToolFinder.Provider("banner", Tools::banner),
                    new ToolFinder.Provider("build", Tools::build),
                    new ToolFinder.Provider("checksum", Tools::checksum),
                    new ToolFinder.Provider("compile", Tools::compile),
                    new ToolFinder.Provider("download", Tools::download),
                    new ToolFinder.Provider("info", Tools::info)),
                ToolFinder.ofSystem())));
  }

  private static final AtomicReference<Bach> INSTANCE = new AtomicReference<>();

  public static Bach getBach() {
    var bach = INSTANCE.get();
    if (bach != null) return bach;
    throw new IllegalStateException();
  }

  public Bach {
    if (!INSTANCE.compareAndSet(null, this)) throw new IllegalStateException();
  }

  public boolean is(Flag flag) {
    return options.flags.contains(flag);
  }

  public void banner(String text) {
    var line = "=".repeat(text.length());
    logbook.out.accept("""
        %s
        %s
        %s""".formatted(line, text, line));
  }

  public void build() {
    run("banner", banner -> banner.with("BUILD"));
    run("info");
    run("compile");
  }

  public void info() {
    var out = logbook.out();
    out.accept("bach.paths = %s".formatted(paths));
    if (!is(Flag.VERBOSE)) return;

    tools.finder().tree(out);

    Stream.of(
            ToolCall.of("jar").with("--version"),
            ToolCall.of("javac").with("--version"),
            ToolCall.of("javadoc").with("--version"))
        .sequential()
        .forEach(call -> run(call, true));

    Stream.of(
            ToolCall.of("jdeps").with("--version"),
            ToolCall.of("jlink").with("--version"),
            ToolCall.of("jmod").with("--version"),
            ToolCall.of("jpackage").with("--version"))
        .parallel()
        .forEach(call -> run(call, true));
  }

  public void checksum(Path path, String algorithm) {
    var checksum = PathSupport.computeChecksum(path, algorithm);
    logbook.out().accept("%s %s".formatted(checksum, path));
  }

  public void checksum(Path path, String algorithm, String expected) {
    var computed = PathSupport.computeChecksum(path, algorithm);
    if (computed.equalsIgnoreCase(expected)) return;
    throw new AssertionError("""
        Checksum %s mismatch for %s!
          computed: %s
          expected: %s
        """.formatted(algorithm, path, computed, expected));
  }

  public void compile() {
    logbook.log(Level.WARNING, "TODO compile()");
  }

  public void download(Map<Path, URI> map) {
    map.entrySet().stream().parallel().forEach(entry -> download(entry.getKey(), entry.getValue()));
  }

  public void download(Path to, URI from) {
    if (Files.exists(to)) return;
    logbook.log(Level.DEBUG, "Downloading %s".formatted(from));
    try (var stream = from.toURL().openStream()) {
      var parent = to.getParent();
      if (parent != null) Files.createDirectories(parent);
      var size = Files.copy(stream, to);
      logbook.log(Level.DEBUG, "Downloaded %,7d %s".formatted(size, to.getFileName()));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  private int main() {
    if (options.calls().isEmpty()) {
      logbook.out().accept("Usage: java Bach.java [OPTIONS] TOOL-NAME [TOOL-ARGS...]");
      logbook.out().accept("Available tools include:");
      tools.finder().tree(logbook.out());
      return 1;
    }
    try (var recording = new Recording()) {
      recording.start();
      logbook.log(Level.DEBUG, "BEGIN");
      options.calls().forEach(call -> run(call, true));
      logbook.log(Level.DEBUG, "END.");
      recording.stop();
      var jfr = Files.createDirectories(paths.out()).resolve("bach-logbook.jfr");
      recording.dump(jfr);
      var logfile = paths.out().resolve("bach-logbook.md");
      logbook.out.accept("-> %s".formatted(jfr.toUri()));
      logbook.out.accept("-> %s".formatted(logfile.toUri()));
      var duration = Duration.between(recording.getStartTime(), recording.getStopTime());
      logbook.out.accept(
          "Run took %d.%02d seconds".formatted(duration.toSeconds(), duration.toMillis()));
      Files.write(logfile, logbook.toMarkdownLines());
      return 0;
    } catch (Exception exception) {
      logbook.log(Level.ERROR, exception.toString());
      return -1;
    }
  }

  public void run(String name, Object... arguments) {
    run(ToolCall.of(name, arguments));
  }

  public void run(String name, UnaryOperator<ToolCall> operator) {
    run(operator.apply(ToolCall.of(name)));
  }

  public void run(ToolCall call) {
    run(call, is(Flag.VERBOSE));
  }

  public void run(ToolCall call, boolean verbose) {
    var event = new RunEvent();
    event.name = call.name();
    event.args = String.join(" ", call.arguments());

    /* Log tool call as a single line */ {
      var line = new StringJoiner(" ");
      line.add(event.name);
      if (!event.args.isEmpty()) {
        var arguments =
            verbose || event.args.length() <= 50
                ? event.args
                : event.args.substring(0, 45) + "[...]";
        line.add(arguments);
      }
      logbook.log(Level.INFO, line.toString());
    }

    var start = Instant.now();
    var tool = tools.finder().find(call.name()).orElseThrow();
    var out = new StringWriter();
    var err = new StringWriter();
    var args = call.arguments().toArray(String[]::new);

    event.begin();
    event.code = tool.run(new PrintWriter(out), new PrintWriter(err), args);
    event.end();
    event.out = out.toString().strip();
    event.err = err.toString().strip();
    event.commit();

    if (verbose) {
      if (!event.out.isEmpty()) logbook.out().accept(event.out.indent(2).stripTrailing());
      if (!event.err.isEmpty()) logbook.err().accept(event.err.indent(2).stripTrailing());
      var duration = Duration.between(start, Instant.now());
      var line =
          "%s ran %d.%02d seconds and returned code %d"
              .formatted(call.name(), duration.toSeconds(), duration.toMillis(), event.code);
      var printer = event.code == 0 ? logbook.out() : logbook().err();
      printer.accept(line);
    }
  }

  public record Paths(Path root, Path out) {}

  public record Externals(Map<Path, URI> programs) {}

  public record Tools(ToolFinder finder) {
    static int banner(PrintWriter out, PrintWriter err, String... args) {
      if (args.length == 0) {
        err.println("Usage: banner TEXT");
        return 1;
      }
      Bach.getBach().banner(String.join(" ", args));
      return 0;
    }

    static int build(PrintWriter out, PrintWriter err, String... args) {
      Bach.getBach().build();
      return 0;
    }

    static int checksum(PrintWriter out, PrintWriter err, String... args) {
      if (args.length < 2 || args.length > 3) {
        err.println("Usage: checksum FILE ALGORITHM [EXPECTED-CHECKSUM]");
        return 1;
      }
      var file = Path.of(args[0]);
      var algorithm = args[1];
      if (args.length == 2) Bach.getBach().checksum(file, algorithm);
      if (args.length == 3) Bach.getBach().checksum(file, algorithm, args[2]);
      return 0;
    }

    static int compile(PrintWriter out, PrintWriter err, String... args) {
      Bach.getBach().compile();
      return 0;
    }

    static int download(PrintWriter out, PrintWriter err, String... args) {
      var bach = Bach.getBach();
      if (args.length == 0) { // everything
        bach.download(bach.externals().programs());
        return 0;
      }
      if (args.length == 2) {
        var to = Path.of(args[0]);
        var from = URI.create(args[1]);
        bach.download(to, from);
        return 0;
      }
      err.println("Usage: download TO-PATH FROM-URI");
      return 1;
    }

    static int info(PrintWriter out, PrintWriter err, String... args) {
      Bach.getBach().info();
      return 0;
    }
  }

  public enum Flag {
    VERBOSE
  }

  public record Options(
      Set<Flag> flags,
      Level __logbook_threshold,
      Path __chroot,
      Path __destination,
      List<ToolCall> calls) {

    static Options of(String... args) {
      var flags = EnumSet.noneOf(Flag.class);
      var level = Level.INFO;
      var root = Path.of("");
      var destination = Path.of(".bach", "out");

      var arguments = new ArrayDeque<>(List.of(args));
      var calls = new ArrayList<ToolCall>();
      while (!arguments.isEmpty()) {
        var argument = arguments.removeFirst();
        if (argument.startsWith("--")) {
          if (argument.equals("--verbose")) {
            flags.add(Flag.VERBOSE);
            continue;
          }
          var delimiter = argument.indexOf('=', 2);
          var key = delimiter == -1 ? argument : argument.substring(0, delimiter);
          var value = delimiter == -1 ? arguments.removeFirst() : argument.substring(delimiter + 1);
          if (key.equals("--logbook-threshold")) {
            level = Level.valueOf(value);
            continue;
          }
          if (key.equals("--chroot")) {
            root = Path.of(value).normalize();
            continue;
          }
          if (key.equals("--destination")) {
            destination = Path.of(value).normalize();
            continue;
          }
          throw new IllegalArgumentException("Unsupported option `%s`".formatted(key));
        }
        calls.add(new ToolCall(argument, arguments.stream().toList()));
        break;
      }
      return new Options(
          Set.copyOf(flags), level, root, root.resolve(destination), List.copyOf(calls));
    }
  }

  public record Logbook(
      Consumer<String> out, Consumer<String> err, Level threshold, Deque<LogEvent> logs) {

    public void log(Level level, String message) {
      var event = new LogEvent();
      event.level = level.name();
      event.message = message;
      event.commit();
      logs.add(event);
      if (level.getSeverity() < threshold.getSeverity()) return;
      var consumer = level.getSeverity() <= Level.INFO.getSeverity() ? out : err;
      consumer.accept(message);
    }

    public List<String> toMarkdownLines() {
      try {
        var lines = new ArrayList<>(List.of("# Logbook"));
        lines.add("");

        lines.add("## Log Events");
        lines.add("");
        lines.add("```text");
        logs.forEach(log -> lines.add("[%c] %s".formatted(log.level.charAt(0), log.message)));
        lines.add("```");
        return List.copyOf(lines);
      } catch (Exception exception) {
        throw new RuntimeException("Failed to read recorded events?", exception);
      }
    }
  }

  public record ToolCall(String name, List<String> arguments) {
    public static ToolCall of(String name, Object... arguments) {
      if (arguments.length == 0) return new ToolCall(name, List.of());
      if (arguments.length == 1) return new ToolCall(name, List.of(arguments[0].toString()));
      return new ToolCall(name, List.of()).with(Stream.of(arguments));
    }

    public ToolCall with(Stream<?> objects) {
      var strings = objects.map(Object::toString);
      return new ToolCall(name, Stream.concat(arguments.stream(), strings).toList());
    }

    public ToolCall with(Object argument) {
      return with(Stream.of(argument));
    }

    public ToolCall with(String key, Object value, Object... values) {
      var call = with(Stream.of(key, value));
      return values.length == 0 ? call : call.with(Stream.of(values));
    }

    public ToolCall withFindFiles(String glob) {
      return withFindFiles(Path.of(""), glob);
    }

    public ToolCall withFindFiles(Path start, String glob) {
      return withFindFiles(start, "glob", glob);
    }

    public ToolCall withFindFiles(Path start, String syntax, String pattern) {
      var syntaxAndPattern = syntax + ':' + pattern;
      var matcher = start.getFileSystem().getPathMatcher(syntaxAndPattern);
      return withFindFiles(start, Integer.MAX_VALUE, matcher);
    }

    public ToolCall withFindFiles(Path start, int maxDepth, PathMatcher matcher) {
      try (var files = Files.find(start, maxDepth, (p, a) -> matcher.matches(p))) {
        return with(files);
      } catch (Exception exception) {
        throw new RuntimeException("Find files failed in: " + start, exception);
      }
    }
  }

  /**
   * A finder of tool providers.
   *
   * <p>What {@link java.lang.module.ModuleFinder ModuleFinder} is to {@link
   * java.lang.module.ModuleReference ModuleReference}, is {@link ToolFinder} to {@link
   * ToolProvider}.
   */
  @FunctionalInterface
  public interface ToolFinder {

    List<ToolProvider> findAll();

    default Optional<ToolProvider> find(String name) {
      return findAll().stream().filter(tool -> tool.name().equals(name)).findFirst();
    }

    default String title() {
      return getClass().getSimpleName();
    }

    default void tree(Consumer<String> out) {
      visit(0, (depth, finder) -> tree(out, depth, finder));
    }

    private void tree(Consumer<String> out, int depth, ToolFinder finder) {
      var indent = "  ".repeat(depth);
      out.accept(indent + finder.title());
      if (finder instanceof CompositeToolFinder) return;
      finder.findAll().stream()
          .sorted(Comparator.comparing(ToolProvider::name))
          .forEach(tool -> out.accept(indent + "  - " + tool.name()));
    }

    default void visit(int depth, BiConsumer<Integer, ToolFinder> visitor) {
      visitor.accept(depth, this);
    }

    static ToolFinder of(ToolProvider... providers) {
      record DirectToolFinder(List<ToolProvider> findAll) implements ToolFinder {
        @Override
        public String title() {
          return "DirectToolFinder (%d)".formatted(findAll.size());
        }
      }
      return new DirectToolFinder(List.of(providers));
    }

    static ToolFinder of(ClassLoader loader) {
      return ToolFinder.of(ServiceLoader.load(ToolProvider.class, loader));
    }

    static ToolFinder of(ServiceLoader<ToolProvider> loader) {
      record ServiceLoaderToolFinder(ServiceLoader<ToolProvider> loader) implements ToolFinder {
        @Override
        public List<ToolProvider> findAll() {
          synchronized (loader) {
            return loader.stream().map(ServiceLoader.Provider::get).toList();
          }
        }
      }

      return new ServiceLoaderToolFinder(loader);
    }

    static ToolFinder ofSystem() {
      return ToolFinder.of(ClassLoader.getSystemClassLoader());
    }

    static ToolFinder ofProperties(Path directory) {
      record PropertiesToolProvider(String name, Properties properties) implements ToolProvider {
        @Override
        public int run(PrintWriter out, PrintWriter err, String... args) {
          var numbers = properties.stringPropertyNames().stream().map(Integer::valueOf).sorted();
          for (var number : numbers.map(Object::toString).map(properties::getProperty).toList()) {
            var lines = number.lines().map(String::trim).toList();
            var name = lines.get(0);
            if (name.toUpperCase().startsWith("GOTO")) throw new Error("GOTO IS TOO BASIC!");
            var call = ToolCall.of(name).with(lines.stream().skip(1));
            Bach.getBach().run(call);
          }
          return 0;
        }
      }

      record PropertiesToolFinder(Path directory) implements ToolFinder {
        @Override
        public String title() {
          return "PropertiesToolFinder (%s)".formatted(directory);
        }

        @Override
        public List<ToolProvider> findAll() {
          if (!Files.isDirectory(directory)) return List.of();
          var list = new ArrayList<ToolProvider>();
          try (var paths = Files.newDirectoryStream(directory, "*.properties")) {
            for (var path : paths) {
              if (Files.isDirectory(path)) continue;
              var filename = path.getFileName().toString();
              var name = filename.substring(0, filename.length() - ".properties".length());
              var properties = PathSupport.properties(path);
              list.add(new PropertiesToolProvider(name, properties));
            }
          } catch (Exception exception) {
            throw new RuntimeException(exception);
          }
          return List.copyOf(list);
        }
      }

      return new PropertiesToolFinder(directory);
    }

    static ToolFinder ofPrograms(Path directory, Path java, String argsfile) {
      record ProgramToolFinder(Path path, Path java, String argsfile) implements ToolFinder {

        @Override
        public List<ToolProvider> findAll() {
          var name = path.normalize().toAbsolutePath().getFileName().toString();
          return find(name).map(List::of).orElseGet(List::of);
        }

        @Override
        public Optional<ToolProvider> find(String name) {
          var directory = path.normalize().toAbsolutePath();
          if (!Files.isDirectory(directory)) return Optional.empty();
          if (!name.equals(directory.getFileName().toString())) return Optional.empty();
          var command = new ArrayList<String>();
          command.add(java.toString());
          var args = directory.resolve(argsfile);
          if (Files.isRegularFile(args)) {
            command.add("@" + args);
            return Optional.of(new ExecuteProgramToolProvider(name, command));
          }
          var jars = PathSupport.list(directory, PathSupport::isJarFile);
          if (jars.size() == 1) {
            command.add("-jar");
            command.add(jars.get(0).toString());
            return Optional.of(new ExecuteProgramToolProvider(name, command));
          }
          var javas = PathSupport.list(directory, PathSupport::isJavaFile);
          if (javas.size() == 1) {
            command.add(javas.get(0).toString());
            return Optional.of(new ExecuteProgramToolProvider(name, command));
          }
          throw new UnsupportedOperationException("Unknown program layout in " + directory.toUri());
        }
      }

      record ProgramsToolFinder(Path path, Path java, String argsfile) implements ToolFinder {
        @Override
        public String title() {
          return "ProgramsToolFinder (%s -> %s)".formatted(path, java);
        }

        @Override
        public List<ToolProvider> findAll() {
          return PathSupport.list(path, Files::isDirectory).stream()
              .map(directory -> new ProgramToolFinder(directory, java, argsfile))
              .map(ToolFinder::findAll)
              .flatMap(List::stream)
              .toList();
        }
      }
      return new ProgramsToolFinder(directory, java, argsfile);
    }

    static ToolFinder compose(ToolFinder... finders) {
      return new CompositeToolFinder(List.of(finders));
    }

    record CompositeToolFinder(List<ToolFinder> finders) implements ToolFinder {
      @Override
      public String title() {
        return "CompositeToolFinder (%d)".formatted(finders.size());
      }

      @Override
      public List<ToolProvider> findAll() {
        return finders.stream().flatMap(finder -> finder.findAll().stream()).toList();
      }

      @Override
      public Optional<ToolProvider> find(String name) {
        for (var finder : finders) {
          var tool = finder.find(name);
          if (tool.isPresent()) return tool;
        }
        return Optional.empty();
      }

      @Override
      public void visit(int depth, BiConsumer<Integer, ToolFinder> visitor) {
        visitor.accept(depth, this);
        depth++;
        for (var finder : finders) finder.visit(depth, visitor);
      }
    }

    record Provider(String name, ToolFunction function) implements ToolProvider {

      @FunctionalInterface
      public interface ToolFunction {
        int run(PrintWriter out, PrintWriter err, String... args);
      }

      @Override
      public int run(PrintWriter out, PrintWriter err, String... args) {
        return function.run(out, err, args);
      }
    }

    record ExecuteProgramToolProvider(String name, List<String> command) implements ToolProvider {

      @Override
      public int run(PrintWriter out, PrintWriter err, String... arguments) {
        var builder = new ProcessBuilder(command);
        builder.command().addAll(List.of(arguments));
        try {
          var process = builder.start();
          new Thread(new StreamLineConsumer(process.getInputStream(), out::println)).start();
          new Thread(new StreamLineConsumer(process.getErrorStream(), err::println)).start();
          return process.waitFor();
        } catch (Exception exception) {
          exception.printStackTrace(err);
          return -1;
        }
      }
    }
  }

  record StreamLineConsumer(InputStream stream, Consumer<String> consumer) implements Runnable {
    public void run() {
      new BufferedReader(new InputStreamReader(stream)).lines().forEach(consumer);
    }
  }

  static final class PathSupport {

    static String computeChecksum(Path path, String algorithm) {
      if (Files.notExists(path)) throw new RuntimeException(path.toString());
      try {
        if ("size".equalsIgnoreCase(algorithm)) return Long.toString(Files.size(path));
        var md = MessageDigest.getInstance(algorithm);
        try (var source = new BufferedInputStream(new FileInputStream(path.toFile()));
            var target = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
          source.transferTo(target);
        }
        return String.format(
            "%0" + (md.getDigestLength() * 2) + "x", new BigInteger(1, md.digest()));
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }

    static Properties properties(Path path) {
      var properties = new Properties();
      if (Files.exists(path)) {
        try {
          properties.load(new FileInputStream(path.toFile()));
        } catch (Exception exception) {
          throw new RuntimeException(exception);
        }
      }
      return properties;
    }

    static boolean isJarFile(Path path) {
      return nameOrElse(path, "").endsWith(".jar") && Files.isRegularFile(path);
    }

    static boolean isJavaFile(Path path) {
      return nameOrElse(path, "").endsWith(".java") && Files.isRegularFile(path);
    }

    static boolean isModuleInfoJavaFile(Path path) {
      return "module-info.java".equals(name(path)) && Files.isRegularFile(path);
    }

    static List<Path> list(Path directory, DirectoryStream.Filter<? super Path> filter) {
      if (Files.notExists(directory)) return List.of();
      var paths = new TreeSet<>(Comparator.comparing(Path::toString));
      try (var stream = Files.newDirectoryStream(directory, filter)) {
        stream.forEach(paths::add);
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
      return List.copyOf(paths);
    }

    static String name(Path path) {
      return nameOrElse(path, null);
    }

    static String nameOrElse(Path path, String defautName) {
      var normalized = path.normalize();
      var candidate = normalized.toString().isEmpty() ? normalized.toAbsolutePath() : normalized;
      var name = candidate.getFileName();
      return Optional.ofNullable(name).map(Path::toString).orElse(defautName);
    }
  }

  @Category("Bach")
  @Name("Bach.LogEvent")
  @Label("Log")
  @StackTrace(false)
  static final class LogEvent extends Event {
    String level;
    String message;
  }

  @Category("Bach")
  @Name("Bach.RunEvent")
  @Label("Run")
  @StackTrace(false)
  static final class RunEvent extends Event {
    String name;
    String args;
    int code;
    String out;
    String err;
  }
}
