import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
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
import java.util.function.Consumer;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.StackTrace;

/** Java Shell Builder. */
public record Bach(
    Component.Printer printer,
    Component.Options options,
    Component.Paths paths,
    Component.Externals externals,
    Component.Tools tools) {

  public static void main(String... args) {
    var bach = Bach.of(args);
    var code = main(bach);
    if (code != 0) System.exit(code);
  }

  private static int main(Bach bach) {
    var seed = bach.options().seed();
    if (seed == null) {
      bach.printer()
          .print(
              """
          Usage: java Bach.java [OPTIONS] TOOL-NAME [TOOL-ARGS...]

          Available tools include:
          """);
      return 1;
    }
    try (var recording = new Recording()) {
      recording.start();
      bach.printer().print("BEGIN");
      try {
        bach.run(seed);
        return 0;
      } catch (RuntimeException exception) {
        bach.printer().error(exception.toString());
        return -1;
      } finally {
        bach.printer().print("END.");
        recording.stop();
        var jfr = Files.createDirectories(bach.paths().out()).resolve("bach-logbook.jfr");
        recording.dump(jfr);
      }
    } catch (Exception exception) {
      bach.printer().error(exception.toString());
      return -2;
    }
  }

  public static Bach of(String... args) {
    return Bach.of(Component.Printer.ofSystem(), args);
  }

  public static Bach of(Consumer<String> printer, String... args) {
    return Bach.of(Component.Printer.of(printer), args);
  }

  public static Bach of(Component.Printer printer, String... args) {
    var options = Component.Options.of(args);
    var properties = Core.PathSupport.properties(options.__chroot.resolve("bach.properties"));
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
        new Component.Paths(options.__chroot, options.__destination),
        new Component.Externals(
            properties.getProperty("bach.externals.default-checksum-algorithm", "SHA-256"),
            programs),
        new Component.Tools(
            ToolFinder.compose(
                ToolFinder.of(
                    Tool.of4("help", Bach.Core::help),
                    Tool.of4("/?", Bach.Core::help),
                    Tool.of4("/save", Bach.Core::save)),
                ToolFinder.ofBasicTools(options.__chroot.resolve(".bach/basic-tools")),
                ToolFinder.ofPrograms(
                    options.__chroot.resolve(".bach/external-tool-program"),
                    Path.of(System.getProperty("java.home"), "bin", "java"),
                    "java.args"),
                ToolFinder.of(
                    Tool.of3("banner", Bach.Core::banner).with(Tool.Flag.HIDDEN),
                    Tool.of3("checksum", Bach.Core::checksum),
                    Tool.of4("download", Bach.Core::download),
                    Tool.of3("load", Bach.Core::load),
                    Tool.of4("load-and-verify", Bach.Core::loadAndVerify),
                    Tool.of4("info", Bach.Core::info),
                    Tool.of3("tree", Bach.Core::tree)),
                ToolFinder.ofSystem(),
                ToolFinder.of(
                    Tool.ofJavaHomeBinaryExecutable("jarsigner"),
                    Tool.ofJavaHomeBinaryExecutable("java"),
                    Tool.ofJavaHomeBinaryExecutable("jdeprscan"),
                    Tool.ofJavaHomeBinaryExecutable("jfr")))));
  }

  public void run(ToolCall call) {
    var name = call.name();
    var arguments = call.arguments();

    var event = new Core.RunEvent();
    event.name = name;
    event.args = String.join(" ", arguments);

    var tool = tools.finder().find(name).orElseThrow(() -> new ToolNotFoundException(name));
    if (!tool.isHidden()) printer.print(arguments.isEmpty() ? name : name + ' ' + event.args);

    var out = new Core.ForwardingStringWriter(line -> printer().print(line));
    var err = new Core.ForwardingStringWriter(line -> printer().error(line));
    var args = arguments.toArray(String[]::new);

    event.begin();
    var toolProvider = tool.provider();
    if (toolProvider instanceof Tool.BachToolProvider provider) {
      event.code = provider.run(this, out.newPrintWriter(), err.newPrintWriter(), args);
    } else {
      event.code = toolProvider.run(out.newPrintWriter(), err.newPrintWriter(), args);
    }
    event.end();
    event.out = out.toString().strip();
    event.err = err.toString().strip();
    event.commit();

    if (event.code == 0) return;

    throw new AssertionError(
        """
        %s returned non-zero exit code: %d
        """
            .formatted(call.name(), event.code));
  }

  /** A component of Bach. */
  public sealed interface Component {
    record Printer(Consumer<String> out, Consumer<String> err, Deque<Text> texts)
        implements Component {

      record Text(Level level, String string) {}

      public static Printer of(Consumer<String> consumer) {
        return new Printer(consumer, consumer, new ConcurrentLinkedDeque<>());
      }

      public static Printer ofSystem() {
        return new Printer(System.out::println, System.err::println, new ConcurrentLinkedDeque<>());
      }

      public void print(String string) {
        texts.add(new Text(Level.INFO, string));
        out.accept(string);
      }

      public void error(String string) {
        texts.add(new Text(Level.ERROR, string));
        err.accept(string);
      }
    }

    enum Flag {
      VERBOSE
    }

    record Options(Set<Flag> flags, Path __chroot, Path __destination, ToolCall seed)
        implements Component {

      static Options of(String... args) {
        var flags = EnumSet.noneOf(Flag.class);
        var root = Path.of("");
        var destination = Path.of(".bach", "out");
        ToolCall seed = null;

        var arguments = new ArrayDeque<>(List.of(args));
        while (!arguments.isEmpty()) {
          var argument = arguments.removeFirst();
          if (argument.startsWith("--")) {
            if (argument.equals("--verbose")) {
              flags.add(Flag.VERBOSE);
              continue;
            }
            var delimiter = argument.indexOf('=', 2);
            var key = delimiter == -1 ? argument : argument.substring(0, delimiter);
            var value =
                delimiter == -1 ? arguments.removeFirst() : argument.substring(delimiter + 1);
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
          seed = new ToolCall(argument, arguments.stream().toList());
          break;
        }
        return new Options(Set.copyOf(flags), root, root.resolve(destination), seed);
      }
    }

    record Paths(Path root, Path out) implements Component {}

    record Externals(String defaultChecksumAlgorithm, Map<Path, URI> programs)
        implements Component {}

    record Tools(ToolFinder finder) implements Component {}
  }

  /** Internal helpers. */
  static final class Core {

    static int banner(PrintWriter out, PrintWriter err, String... args) {
      if (args.length == 0) {
        err.println("Usage: banner TEXT");
        return 1;
      }
      var text = String.join(" ", args);
      var line = "=".repeat(text.length());
      out.println("""
        %s
        %s
        %s""".formatted(line, text, line));
      return 0;
    }

    static int checksum(PrintWriter out, PrintWriter err, String... args) {
      if (args.length < 1 || args.length > 3) {
        err.println("Usage: checksum FILE [ALGORITHM [EXPECTED-CHECKSUM]]");
        return 1;
      }
      var path = Path.of(args[0]);
      var algorithm = args.length > 1 ? args[1] : "SHA-256";
      var computed = Core.PathSupport.computeChecksum(path, algorithm);
      if (args.length == 1 || args.length == 2) {
        out.printf("%s %s%n", computed, path);
        return 0;
      }
      var expected = args[2];
      if (computed.equalsIgnoreCase(expected)) return 0;
      err.printf(
          """
          Checksum mismatch detected!
                 path: %s
            algorithm: %s
             computed: %s
             expected: %s
          """,
          path, algorithm, computed, expected);
      return 2;
    }

    static int download(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      bach.externals()
          .programs()
          .forEach((target, source) -> bach.run(ToolCall.of("load-and-verify", target, source)));
      return 0;
    }

    static int help(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      out.print(
          """
          Usage: java Bach.java [OPTIONS] TOOL-NAME [TOOL-ARGS...]

          Available tools include:
          """);
      bach.tools().finder().findAll().stream()
          .sorted(Comparator.comparing(Tool::name))
          .forEach(tool -> out.printf("  - %s%n", tool.name()));
      return 0;
    }

    static int load(PrintWriter out, PrintWriter err, String... args) {
      if (args.length != 2) {
        err.println("Usage: load TARGET SOURCE");
        return 1;
      }
      var target = Path.of(args[0]);
      var source = URI.create(args[1]);
      if (Files.notExists(target)) {
        out.printf("Loading %s...", source);
        var uri =
            source.getScheme() == null
                ? Path.of(args[1]).normalize().toAbsolutePath().toUri()
                : source;
        try (var stream = uri.toURL().openStream()) {
          var parent = target.getParent();
          if (parent != null) Files.createDirectories(parent);
          var size = Files.copy(stream, target);
          out.printf("Loaded %,12d %s", size, target.getFileName());
        } catch (Exception exception) {
          exception.printStackTrace(err);
          return 2;
        }
      }
      return 0;
    }

    static int loadAndVerify(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      var target = Path.of(args[0]);
      var source = URI.create(args[1]);
      bach.run(ToolCall.of("load", target, source));

      var calls = new ArrayList<ToolCall>();
      Consumer<String> checker =
          string -> {
            var property = Core.StringSupport.parseProperty(string);
            var algorithm = property.key();
            var expected = property.value();
            calls.add(ToolCall.of("checksum", target, algorithm, expected));
          };

      var index = 2;
      while (index < args.length) checker.accept(args[index++]);

      var fragment = source.getFragment();
      var elements = fragment == null ? new String[0] : fragment.split("&");
      for (var element : elements) checker.accept(element);

      if (calls.isEmpty()) throw new IllegalStateException("No expected checksum given.");

      calls.stream().parallel().forEach(bach::run);
      return 0;
    }

    static int info(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      out.printf("bach.options   = %s%n", bach.options());
      out.printf("bach.paths     = %s%n", bach.paths());
      out.printf("bach.externals%n");
      bach.externals().programs().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> out.printf("  %s -> %s%n", entry.getKey(), entry.getValue()));
      out.printf("bach.tools%n");
      bach.tools().finder().findAll().stream()
          .sorted(Comparator.comparing(Tool::name))
          .forEach(tool -> out.printf("  - %s%n", tool.name()));
      return 0;
    }

    static int save(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      var usage =
          """
        Usage: save TARGET VERSION

        Examples:
          save Bach.java 1.0
          save .bach/Bach-HEAD.java HEAD
        """;
      if (args.length == 1 && args[0].equalsIgnoreCase("--help")) {
        out.print(usage);
        return 0;
      }
      if (args.length != 2) {
        err.print(usage);
        return 1;
      }
      var target = Path.of(args[0]);
      var version = args[1];
      var from = URI.create("https://github.com/sormuras/bach/raw/" + version + "/src/Bach.java");
      out.printf("<< %s%n", from);
      bach.run(ToolCall.of("download").with("--replace-existing").with(target).with(from));
      out.printf(">> %s%n", target);
      return 0;
    }

    static int tree(PrintWriter out, PrintWriter err, String... args) {
      enum Mode {
        CREATE,
        CLEAN,
        DELETE,
        PRINT
      }
      if (args.length != 2) {
        out.println("""
          Usage: tree create|clean|delete|print PATH
          """);
        return 1;
      }
      var mode = Mode.valueOf(args[0].toUpperCase());
      var path = Path.of(args[1]);
      try {
        if (mode == Mode.PRINT) {
          try (var stream = Files.walk(path)) {
            stream
                .filter(Files::isDirectory)
                .map(Path::normalize)
                .map(Path::toString)
                .map(name -> name.replace('\\', '/'))
                .filter(name -> !name.contains(".git/"))
                .sorted()
                .map(name -> name.replaceAll(".+?/", "  "))
                .forEach(out::println);
          }
        }
        if (mode == Mode.DELETE || mode == Mode.CLEAN) {
          Files.createDirectories(path);
        }
        if (mode == Mode.CREATE || mode == Mode.CLEAN) {
          try (var stream = Files.walk(path)) {
            var files = stream.sorted((p, q) -> -p.compareTo(q));
            for (var file : files.toArray(Path[]::new)) Files.deleteIfExists(file);
          }
        }
      } catch (Exception exception) {
        exception.printStackTrace(err);
        return 2;
      }
      return 0;
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
        string.substring(beginIndex).lines().forEach(consumer);
        beginIndex = length;
      }

      PrintWriter newPrintWriter() {
        return new PrintWriter(this, true);
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

  /** A command consisting of a tool name and a list of arguments. */
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

  public record Tool(Set<Flag> flags, String name, ToolProvider provider) {

    public enum Flag {
      HIDDEN
    }

    @FunctionalInterface
    public interface Provider extends ToolProvider {
      @Override
      default String name() {
        return getClass().getSimpleName();
      }
    }

    @FunctionalInterface
    public interface BachToolProvider extends Provider {
      @Override
      default int run(PrintWriter out, PrintWriter err, String... args) {
        throw new UnsupportedOperationException();
      }

      @Override
      default int run(PrintStream out, PrintStream err, String... args) {
        throw new UnsupportedOperationException();
      }

      int run(Bach bach, PrintWriter out, PrintWriter err, String... args);
    }

    public static Tool of(ToolProvider provider) {
      return new Tool(Set.of(), provider.name(), provider);
    }

    public static Tool of3(String name, Provider provider) {
      record LocalProvider(Provider provider) implements Provider {
        @Override
        public int run(PrintWriter out, PrintWriter err, String... args) {
          return provider.run(out, err, args);
        }
      }
      return new Tool(Set.of(), name, new LocalProvider(provider));
    }

    public static Tool of4(String name, BachToolProvider provider) {
      record LocalProvider(BachToolProvider provider) implements BachToolProvider {
        @Override
        public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
          return provider.run(bach, out, err, args);
        }
      }
      return new Tool(Set.of(), name, new LocalProvider(provider));
    }

    public static Tool ofJavaHomeBinaryExecutable(String name) {
      var executable = Path.of(System.getProperty("java.home"), "bin", name);
      return Tool.of(new ExecutableToolProvider(name, List.of(executable.toString())));
    }

    public static Tool ofExecutable(String name, List<String> command) {
      return Tool.of(new ExecutableToolProvider(name, command));
    }

    public Tool with(Flag flag) {
      var flags = Stream.concat(this.flags.stream(), Stream.of(flag)).toList();
      return new Tool(Set.copyOf(flags), name, provider);
    }

    public boolean isHidden() {
      return flags.contains(Flag.HIDDEN);
    }



    record ExecutableToolProvider(String name, List<String> command) implements ToolProvider {

      record LinePrinter(InputStream stream, PrintWriter writer) implements Runnable {
        @Override
        public void run() {
          new BufferedReader(new InputStreamReader(stream)).lines().forEach(writer::println);
        }
      }

      @Override
      public int run(PrintWriter out, PrintWriter err, String... arguments) {
        var builder = new ProcessBuilder(new ArrayList<>(command));
        builder.command().addAll(List.of(arguments));
        try {
          var process = builder.start();
          new Thread(new LinePrinter(process.getInputStream(), out)).start();
          new Thread(new LinePrinter(process.getErrorStream(), err)).start();
          return process.waitFor();
        } catch (Exception exception) {
          exception.printStackTrace(err);
          return -1;
        }
      }
    }
  }

  /**
   * A finder of tools.
   *
   * <p>What {@link java.lang.module.ModuleFinder ModuleFinder} is to {@link
   * java.lang.module.ModuleReference ModuleReference}, is {@link ToolFinder} to {@link Tool}.
   */
  @FunctionalInterface
  public interface ToolFinder {

    List<Tool> findAll();

    default Optional<Tool> find(String name) {
      return findAll().stream().filter(tool -> tool.name().equals(name)).findFirst();
    }

    static ToolFinder of(Tool... tools) {
      record DirectToolFinder(List<Tool> findAll) implements ToolFinder {}
      return new DirectToolFinder(List.of(tools));
    }

    static ToolFinder of(ClassLoader loader) {
      return ToolFinder.of(ServiceLoader.load(ToolProvider.class, loader));
    }

    static ToolFinder of(ServiceLoader<ToolProvider> loader) {
      record ServiceLoaderToolFinder(ServiceLoader<ToolProvider> loader) implements ToolFinder {
        @Override
        public List<Tool> findAll() {
          synchronized (loader) {
            return loader.stream().map(ServiceLoader.Provider::get).map(Tool::of).toList();
          }
        }
      }

      return new ServiceLoaderToolFinder(loader);
    }

    static ToolFinder ofSystem() {
      return ToolFinder.of(ClassLoader.getSystemClassLoader());
    }

    static ToolFinder ofBasicTools(Path directory) {
      record BasicToolProvider(String name, Properties properties) implements Tool.BachToolProvider {
        @Override
        public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
          var verbose = bach.options().flags().contains(Component.Flag.VERBOSE);
          if (verbose) out.println("BEGIN # of " + name);
          var numbers =
              properties.stringPropertyNames().stream()
                  .sorted(Comparator.comparing(Integer::parseInt))
                  .toList();
          for (var number : numbers) {
            if (verbose) out.println(name + ':' + number);
            var value = properties.getProperty(number);
            var lines = value.lines().map(line -> replace(bach, line)).toList();
            var name = lines.get(0);
            if (name.toUpperCase().startsWith("GOTO")) throw new Error("GOTO IS TOO BASIC!");
            var call = ToolCall.of(name).with(lines.stream().skip(1));
            bach.run(call);
          }
          if (verbose) out.println("END # of " + name);
          return 0;
        }

        private String replace(Bach bach, String line) {
          return line.trim()
              .replace("{{bach.paths.root}}", Core.PathSupport.normalized(bach.paths.root))
              .replace("{{bach.paths.out}}", Core.PathSupport.normalized(bach.paths.out))
              .replace("{{path.separator}}", System.getProperty("path.separator"));
        }
      }

      record BasicToolFinder(Path directory) implements ToolFinder {
        @Override
        public List<Tool> findAll() {
          if (!Files.isDirectory(directory)) return List.of();
          var list = new ArrayList<Tool>();
          try (var paths = Files.newDirectoryStream(directory, "*.properties")) {
            for (var path : paths) {
              if (Files.isDirectory(path)) continue;
              var filename = path.getFileName().toString();
              var name = filename.substring(0, filename.length() - ".properties".length());
              var properties = Core.PathSupport.properties(path);
              list.add(Tool.of(new BasicToolProvider(name, properties)));
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
        public List<Tool> findAll() {
          var name = path.normalize().toAbsolutePath().getFileName().toString();
          return find(name).map(List::of).orElseGet(List::of);
        }

        @Override
        public Optional<Tool> find(String name) {
          var directory = path.normalize().toAbsolutePath();
          if (!Files.isDirectory(directory)) return Optional.empty();
          if (!name.equals(directory.getFileName().toString())) return Optional.empty();
          var command = new ArrayList<String>();
          command.add(java.toString());
          var args = directory.resolve(argsfile);
          if (Files.isRegularFile(args)) {
            command.add("@" + args);
            return Optional.of(Tool.ofExecutable(name, command));
          }
          var jars = Core.PathSupport.list(directory, Core.PathSupport::isJarFile);
          if (jars.size() == 1) {
            command.add("-jar");
            command.add(jars.get(0).toString());
            return Optional.of(Tool.ofExecutable(name, command));
          }
          var javas = Core.PathSupport.list(directory, Core.PathSupport::isJavaFile);
          if (javas.size() == 1) {
            command.add(javas.get(0).toString());
            return Optional.of(Tool.ofExecutable(name, command));
          }
          throw new UnsupportedOperationException("Unknown program layout in " + directory.toUri());
        }
      }

      record ProgramsToolFinder(Path path, Path java, String argsfile) implements ToolFinder {
        @Override
        public List<Tool> findAll() {
          return Core.PathSupport.list(path, Files::isDirectory).stream()
              .map(directory -> new ProgramToolFinder(directory, java, argsfile))
              .map(ToolFinder::findAll)
              .flatMap(List::stream)
              .toList();
        }
      }
      return new ProgramsToolFinder(directory, java, argsfile);
    }

    static ToolFinder compose(ToolFinder... finders) {
      record CompositeToolFinder(List<ToolFinder> finders) implements ToolFinder {
        @Override
        public List<Tool> findAll() {
          return finders.stream().flatMap(finder -> finder.findAll().stream()).toList();
        }

        @Override
        public Optional<Tool> find(String name) {
          for (var finder : finders) {
            var tool = finder.find(name);
            if (tool.isPresent()) return tool;
          }
          return Optional.empty();
        }
      }
      return new CompositeToolFinder(List.of(finders));
    }
  }

  public static final class ToolNotFoundException extends RuntimeException {
    @java.io.Serial private static final long serialVersionUID = -417539767734303099L;

    ToolNotFoundException(String name) {
      super("Tool named `%s` not found".formatted(name));
    }
  }
}
