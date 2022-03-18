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
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedDeque;
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
    Printer printer, Options options, Paths paths, Externals externals, Tools tools) {

  public static void main(String... args) {
    var bach = Bach.of(args);
    var code = bach.main();
    System.exit(code);
  }

  public static Bach of(String... args) {
    return Bach.of(Printer.ofSystem(), args);
  }

  public static Bach of(Printer printer, String... args) {
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
        printer,
        options,
        new Paths(options.__chroot, options.__destination),
        new Externals(
            properties.getProperty("bach.externals.default-checksum-algorithm", "SHA-256"),
            programs),
        new Tools(
            ToolFinder.compose(
                ToolFinder.of(Tool.of("help", Tool::help), Tool.of("/?", Tool::help)),
                ToolFinder.ofBasicTools(options.__chroot.resolve(".bach/basic-tools")),
                ToolFinder.ofPrograms(
                    options.__chroot.resolve(".bach/external-tool-program"),
                    Path.of(System.getProperty("java.home"), "bin", "java"),
                    "java.args"),
                ToolFinder.of(
                    Tool.of("banner", Tool::banner),
                    Tool.of("build", Tool::build),
                    Tool.of("checksum", Tool::checksum),
                    Tool.of("compile", Tool::compile),
                    Tool.of("download", Tool::download),
                    Tool.of("info", Tool::info),
                    Tool.of("test", Tool::test)),
                ToolFinder.ofSystem())));
  }

  public boolean is(Flag flag) {
    return options.flags.contains(flag);
  }

  public void banner(String text) {
    var line = "=".repeat(text.length());
    printer.print("""
        %s
        %s
        %s""".formatted(line, text, line));
  }

  public void build() {
    run("banner", banner -> banner.with("BUILD"));
    run("info");
    run("compile");
    run("test");
  }

  public void info() {
    printer.print("bach.paths = %s".formatted(paths));

    if (!is(Flag.VERBOSE)) return;

    Stream.of(
            ToolCall.of("jar").with("--version"),
            ToolCall.of("javac").with("--version"),
            ToolCall.of("javadoc").with("--version"))
        .parallel()
        .forEach(this::run);

    Stream.of(
            ToolCall.of("jdeps").with("--version"),
            ToolCall.of("jlink").with("--version"),
            ToolCall.of("jmod").with("--version"),
            ToolCall.of("jpackage").with("--version"))
        .sequential()
        .forEach(this::run);
  }

  public void help() {
    printer.print(
        """
        Usage: java Bach.java [OPTIONS] TOOL-NAME [TOOL-ARGS...]

        Available tools include:
        """);
    tools.finder().tree(printer.out());
  }

  public void checksum(Path path) {
    run("checksum", path, externals.defaultChecksumAlgorithm());
  }

  public void checksum(Path path, String algorithm) {
    var checksum = PathSupport.computeChecksum(path, algorithm);
    printer.print("%s %s".formatted(checksum, path));
  }

  public void checksum(Path path, String algorithm, String expected) {
    var computed = PathSupport.computeChecksum(path, algorithm);
    if (computed.equalsIgnoreCase(expected)) return;
    throw new AssertionError(
        """
        Checksum mismatch detected!
               path: %s
          algorithm: %s
           computed: %s
           expected: %s
        """
            .formatted(path, algorithm, computed, expected));
  }

  public void compile() {
    log(Level.WARNING, "TODO compile()");
  }

  public void download(Map<Path, URI> map) {
    map.entrySet().stream()
        .parallel()
        .forEach(entry -> run("download", entry.getKey(), entry.getValue()));
  }

  public void download(Path to, URI from) {
    if (Files.notExists(to)) {
      log("Downloading %s".formatted(from));
      try (var stream = from.toURL().openStream()) {
        var parent = to.getParent();
        if (parent != null) Files.createDirectories(parent);
        var size = Files.copy(stream, to);
        log("Downloaded %,12d %s".formatted(size, to.getFileName()));
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
    var fragment = from.getFragment();
    if (fragment == null) return;
    for (var element : fragment.split("&")) {
      var property = StringSupport.parseProperty(element);
      var algorithm = property.key();
      var expected = property.value();
      run("checksum", to, algorithm, expected);
    }
  }

  public void log(String message) {
    log(Level.DEBUG, message);
  }

  public void log(Level level, String message) {
    var event = new LogEvent();
    event.level = level.name();
    event.message = message;
    event.commit();

    var severity = level.getSeverity();
    if (severity < options.__logbook_threshold.getSeverity()) return;
    if (severity <= Level.DEBUG.getSeverity()) {
      printer.print(message);
      return;
    }
    if (severity >= Level.ERROR.getSeverity()) {
      printer.error(message);
      return;
    }
    var max = 1000;
    printer.print(message.length() <= max ? message : message.substring(0, max - 5) + "[...]");
  }

  private int main() {
    if (options.calls().isEmpty()) {
      help();
      return 1;
    }
    try (var recording = new Recording()) {
      recording.start();
      log("BEGIN");
      try {
        options.calls().forEach(call -> run(call, Level.DEBUG));
        return 0;
      } catch (RuntimeException exception) {
        log(Level.ERROR, exception.toString());
        return -1;
      } finally {
        log("END.");
        recording.stop();
        var jfr = Files.createDirectories(paths.out()).resolve("bach-logbook.jfr");
        recording.dump(jfr);
      }
    } catch (Exception exception) {
      log(Level.ERROR, exception.toString());
      return -2;
    }
  }

  public void run(String name, Object... arguments) {
    run(ToolCall.of(name, arguments));
  }

  public void run(String name, UnaryOperator<ToolCall> operator) {
    run(operator.apply(ToolCall.of(name)));
  }

  public void run(ToolCall call) {
    run(call, Level.INFO);
  }

  void run(ToolCall call, Level level) {
    var name = call.name();
    var arguments = call.arguments();

    var event = new RunEvent();
    event.name = name;
    event.args = String.join(" ", arguments);

    log(level, arguments.isEmpty() ? name : name + ' ' + event.args);

    var tool = tools.finder().find(name).orElseThrow(() -> new ToolNotFoundException(name));
    var out = new ForwardingStringWriter(s -> printer().out().accept(s.indent(2).stripTrailing()));
    var err = new ForwardingStringWriter(s -> printer().err().accept(s.indent(2).stripTrailing()));
    var args = arguments.toArray(String[]::new);

    event.begin();
    event.code =
        tool instanceof Tool.Provider provider
            ? provider.run(this, new PrintWriter(out, true), new PrintWriter(err, true), args)
            : tool.run(new PrintWriter(out, true), new PrintWriter(err, true), args);
    event.end();
    event.out = out.toString().strip();
    event.err = err.toString().strip();
    event.commit();

    if (event.code == 0) return;

    throw new AssertionError(
        """
        %s returned non-zero exit code: %d
        %s
        %s
        """
            .formatted(call.name(), event.code, event.err, event.out));
  }

  public void test() {
    log(Level.WARNING, "TODO test()");
  }

  public record Printer(Consumer<String> out, Consumer<String> err, Deque<Line> lines) {

    record Line(Level level, String text) {}

    public static Printer ofSilent() {
      return new Printer(__ -> {}, __ -> {}, new ConcurrentLinkedDeque<>());
    }

    public static Printer ofSystem() {
      return new Printer(System.out::println, System.err::println, new ConcurrentLinkedDeque<>());
    }

    public void print(String string) {
      lines.add(new Line(Level.INFO, string));
      out.accept(string);
    }

    public void error(String string) {
      lines.add(new Line(Level.ERROR, string));
      err.accept(string);
    }
  }

  @FunctionalInterface
  public interface Tool {

    int run(Bach bach, PrintWriter out, PrintWriter err, String... args);

    interface Provider extends Tool, ToolProvider {
      @Override
      default int run(PrintWriter out, PrintWriter err, String... args) {
        return run(Bach.of(Printer.ofSilent()), out, err, args);
      }
    }

    static Provider of(String name, Tool tool) {
      record Wrapper(String name, Tool tool) implements Provider {
        @Override
        public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
          return tool.run(bach, out, err, args);
        }
      }
      return new Wrapper(name, tool);
    }

    private static int banner(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      if (args.length == 0) {
        err.println("Usage: banner TEXT");
        return 1;
      }
      bach.banner(String.join(" ", args));
      return 0;
    }

    private static int build(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      bach.build();
      return 0;
    }

    private static int checksum(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      if (args.length < 1 || args.length > 3) {
        err.println("Usage: checksum FILE [ALGORITHM [EXPECTED-CHECKSUM]]");
        return 1;
      }
      var file = Path.of(args[0]);
      switch (args.length) {
        case 1 -> bach.checksum(file);
        case 2 -> bach.checksum(file, args[1]);
        case 3 -> bach.checksum(file, args[1], args[2]);
      }
      return 0;
    }

    private static int compile(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      bach.compile();
      return 0;
    }

    private static int download(Bach bach, PrintWriter out, PrintWriter err, String... args) {
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

    private static int help(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      bach.help();
      return 0;
    }

    private static int info(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      bach.info();
      return 0;
    }

    private static int test(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      bach.test();
      return 0;
    }
  }

  public record Paths(Path root, Path out) {}

  public record Externals(String defaultChecksumAlgorithm, Map<Path, URI> programs) {}

  public record Tools(ToolFinder finder) {}

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

    static ToolFinder ofBasicTools(Path directory) {
      record BasicToolProvider(String name, Properties properties) implements Tool.Provider {
        @Override
        public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
          var commands =
              properties.stringPropertyNames().stream()
                  .sorted(Comparator.comparing(Integer::parseInt))
                  .map(properties::getProperty)
                  .toList();
          for (var command : commands) {
            var lines = command.lines().map(line -> replace(bach, line)).toList();
            var name = lines.get(0);
            if (name.toUpperCase().startsWith("GOTO")) throw new Error("GOTO IS TOO BASIC!");
            var call = ToolCall.of(name).with(lines.stream().skip(1));
            bach.run(call);
          }
          return 0;
        }

        private String replace(Bach bach, String line) {
          return line.trim()
              .replace("{{bach.paths.root}}", PathSupport.normalized(bach.paths.root))
              .replace("{{bach.paths.out}}", PathSupport.normalized(bach.paths.out))
              .replace("{{path.separator}}", System.getProperty("path.separator"));
        }
      }

      record BasicToolFinder(Path directory) implements ToolFinder {
        @Override
        public String title() {
          return "BasicToolFinder (%s)".formatted(directory);
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
              list.add(new BasicToolProvider(name, properties));
            }
          } catch (Exception exception) {
            throw new RuntimeException(exception);
          }
          return List.copyOf(list);
        }
      }

      return new BasicToolFinder(directory);
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

  static final class ForwardingStringWriter extends StringWriter {

    private int beginIndex = 0;
    private final Consumer<String> consumer;

    ForwardingStringWriter(Consumer<String> consumer) {
      this.consumer = consumer;
    }

    @Override
    public void flush() {
      var buffer = getBuffer();
      if (buffer.isEmpty()) return;
      var string = buffer.toString();
      var length = string.length();
      if (beginIndex >= length) return;
      consumer.accept(string.substring(beginIndex));
      beginIndex = length;
    }
  }

  static final class PathSupport {

    static String computeChecksum(Path path, String algorithm) {
      if (Files.notExists(path)) throw new RuntimeException("File not found: " + path);
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
      return name != null ? name.toString() : defautName;
    }

    static String normalized(Path path) {
      var string = path.normalize().toString();
      return string.isEmpty() ? "." : string;
    }
  }

  static final class StringSupport {
    record Property(String key, String value) {}

    static Property parseProperty(String string) {
      return StringSupport.parseProperty(string, '=');
    }

    static Property parseProperty(String string, char separator) {
      int index = string.indexOf(separator);
      if (index < 0) {
        var message = "Expected a `KEY%sVALUE` string, but got: %s".formatted(separator, string);
        throw new IllegalArgumentException(message);
      }
      var key = string.substring(0, index);
      var value = string.substring(index + 1);
      return new Property(key, value);
    }
  }

  static final class ToolNotFoundException extends RuntimeException {
    @java.io.Serial private static final long serialVersionUID = -417539767734303099L;

    public ToolNotFoundException(String name) {
      super("Tool named `%s` not found".formatted(name));
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
