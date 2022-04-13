import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.StackTrace;

/** Java Shell Builder. */
public final class Bach {

  /** Bach's main program running the initial seed tool call. */
  public static void main(String... args) {
    var bach = Bach.of(args);
    var code = bach.main();
    if (code != 0) System.exit(code);
  }

  /** {@return an instance with "standard" streams and configured from the given arguments array} */
  public static Bach of(String... args) {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    return new Bach(Configuration.of(out, err, args));
  }

  private final Configuration configuration;
  private final ThreadLocal<Deque<String>> stackedToolCallNames;

  /** Initialize this {@code Bach} instance. */
  public Bach(Configuration configuration) {
    this.configuration = configuration;
    this.stackedToolCallNames = ThreadLocal.withInitial(ArrayDeque::new);
  }

  /** {@return the immutable configuration object} */
  public Configuration configuration() {
    return configuration;
  }

  /** {@return the result of running the initial seed tool call} */
  private int main() {
    var verbose = configuration.isVerbose();
    var seed = configuration.tools.seed;
    if (seed == null) {
      run(Tool.Call.of("/?"));
      return 1;
    }
    var printer = configuration.printer;
    var paths = configuration.paths;
    try (var recording = new Recording()) {
      recording.start();
      try {
        if (verbose) printer.out("BEGIN");
        run(seed);
        if (verbose) printer.out("END.");
        return 0;
      } catch (RuntimeException exception) {
        printer.err(exception.getClass().getSimpleName() + ": " + exception.getMessage());
        return 2;
      } finally {
        recording.stop();
        var jfr = Files.createDirectories(paths.out).resolve("bach-logbook.jfr");
        recording.dump(jfr);
      }
    } catch (Exception exception) {
      exception.printStackTrace(printer.err);
      return -2;
    }
  }

  /** Run the given tool call. */
  public void run(Tool.Call call) {
    run(call.name(), call.arguments());
  }

  /** Run the tool specified the given name and passing the given arguments. */
  public void run(String name, Object... arguments) {
    run(name, Stream.of(arguments).map(Object::toString).toList());
  }

  /** Run the tool specified by its name and passing the given arguments. */
  public void run(String name, List<String> arguments) {
    var verbose = configuration.isVerbose();
    var printer = configuration.printer;
    var finder = configuration.tools.finder;

    var tools = finder.find(name);
    if (tools.isEmpty()) throw new ToolNotFoundException(name);
    if (tools.size() != 1) throw new ToolNotUniqueException(name, tools);
    var tool = tools.get(0);

    var names = stackedToolCallNames.get();
    try {
      names.addLast(name);
      if (tool.isNotHidden()) {
        if (verbose && names.size() > 1) printer.out(String.join(" | ", names));
        printer.out(arguments.isEmpty() ? name : name + ' ' + String.join(" ", arguments));
      }
      var code = run(tool.provider(), name, arguments);
      if (code != 0) {
        throw new RuntimeException("%s returned non-zero exit code: %d".formatted(name, code));
      }
    } finally {
      names.removeLast();
    }
  }

  private int run(ToolProvider provider, String name, List<String> arguments) {
    var event = new Core.RunEvent();
    event.name = name;
    event.args = String.join(" ", arguments);
    var printer = configuration.printer;
    try (var out = new Core.MirroringStringPrintWriter(printer.out);
        var err = new Core.MirroringStringPrintWriter(printer.err)) {
      var args = arguments.toArray(String[]::new);
      event.begin();
      if (provider instanceof Tool.Operator operator) {
        event.code = operator.run(this, out, err, args);
      } else {
        event.code = provider.run(out, err, args);
      }
      event.end();
      event.out = out.toString().strip();
      event.err = err.toString().strip();
      event.commit();
      return event.code;
    } finally {
      printer.out.flush();
      printer.err.flush();
    }
  }

  /** Immutable settings. */
  public record Configuration(
      Printer printer, Flags flags, Paths paths, Tools tools, Project project) {
    enum Flag {
      VERBOSE
    }

    public static Configuration of(PrintWriter out, PrintWriter err, String... args) {
      var printer = new Printer(out, err);
      var flags = EnumSet.noneOf(Flag.class);
      var root = Path.of("");
      Tool.Call seed = null;

      var arguments = new ArrayDeque<>(List.of(args));
      while (!arguments.isEmpty()) {
        var argument = arguments.removeFirst();
        if (argument.startsWith("--")) {
          if (argument.equals("--verbose")) {
            flags.add(Flag.VERBOSE);
            continue;
          }
          var index = argument.indexOf('=', 2);
          var key = index == -1 ? argument : argument.substring(0, index);
          var value = index == -1 ? arguments.removeFirst() : argument.substring(index + 1);
          if (key.equals("--chroot")) {
            root = Path.of(value).normalize();
            continue;
          }
          throw new IllegalArgumentException("Unsupported option `%s`".formatted(key));
        }
        seed = new Tool.Call(argument, arguments.stream().toList());
        break;
      }

      var paths = new Paths(root, root.resolve(Path.of(".bach", "out")));
      var finder =
          Tool.Finder.compose(
              Tool.Finder.of(
                  Tool.of("bach/help", Core::help),
                  Tool.of("/?", Core::help),
                  Tool.of("/save", Core::save)),
              Tool.Finder.ofBasicTools(paths.root(".bach", "basic-tools")),
              Tool.Finder.ofJavaTools(
                  paths.root(".bach", "external-tools"),
                  Path.of(System.getProperty("java.home"), "bin", "java"),
                  "java.args"),
              Tool.Finder.of(
                  Tool.of("bach/banner", Core::banner).with(Tool.Flag.HIDDEN),
                  Tool.of("bach/checksum", Core::checksum),
                  Tool.of("bach/load", Core::load),
                  Tool.of("bach/load-and-verify", Core::loadAndVerify),
                  Tool.of("bach/info", Core::info),
                  Tool.of("bach/tree", Core::tree)),
              Tool.Finder.of(Tool.of("project/compile", Project::compile)),
              Tool.Finder.ofSystemTools(),
              Tool.Finder.of(
                  Tool.ofNativeToolInJavaHome("jarsigner"),
                  Tool.ofNativeToolInJavaHome("java").with(Tool.Flag.HIDDEN),
                  Tool.ofNativeToolInJavaHome("jdeprscan"),
                  Tool.ofNativeToolInJavaHome("jfr")));
      var tools = new Tools(finder, seed);

      return new Configuration(printer, new Flags(Set.copyOf(flags)), paths, tools, new Project());
    }

    public boolean isVerbose() {
      return is(Flag.VERBOSE);
    }

    public boolean is(Flag flag) {
      return flags.set.contains(flag);
    }

    public record Flags(Set<Flag> set) {}

    public record Printer(PrintWriter out, PrintWriter err) {
      void out(String string) {
        out.println(string);
      }

      void err(String string) {
        err.println(string);
      }
    }

    public record Paths(Path root, Path out) {
      public Path root(String first, String... more) {
        return root.resolve(Path.of(first, more));
      }

      public Path out(String first, String... more) {
        return out.resolve(Path.of(first, more));
      }
    }

    public record Tools(Tool.Finder finder, Tool.Call seed) {}
  }

  /** Internal helpers. */
  static final class Core {

    static int banner(Bach bach, PrintWriter out, PrintWriter err, String... args) {
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

    static int checksum(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      if (args.length < 1 || args.length > 3) {
        err.println("Usage: checksum FILE [ALGORITHM [EXPECTED-CHECKSUM]]");
        return 1;
      }
      var path = Path.of(args[0]);
      var algorithm = args.length > 1 ? args[1] : "SHA-256";
      var computed = PathSupport.computeChecksum(path, algorithm);
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

    static int help(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      out.print(
          """
          Usage: java Bach.java [OPTIONS] TOOL-NAME [TOOL-ARGS...]
          """);
      if (bach.configuration.isVerbose()) {
        out.println("Available tools include:");
        bach.configuration.tools.finder.findAll().stream()
            .filter(Tool::isNotHidden)
            .sorted(Comparator.comparing(Tool::name))
            .forEach(tool -> out.printf("  - %s%n", tool.name()));
      }
      return 0;
    }

    static int load(Bach bach, PrintWriter out, PrintWriter err, String... args) {
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
      bach.run(Tool.Call.of("load", target, source));

      var calls = new ArrayList<Tool.Call>();
      Consumer<String> checker =
          string -> {
            var property = StringSupport.parseProperty(string);
            var algorithm = property.key();
            var expected = property.value();
            calls.add(Tool.Call.of("checksum", target, algorithm, expected));
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
      out.println("Info");
      out.println(bach.configuration.flags);
      out.println(bach.configuration.paths);
      out.printf("Tools%n");
      bach.configuration.tools.finder.findAll().stream()
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
      bach.run("load", /* TODO "--replace-existing", */ target, from);
      out.printf(">> %s%n", target);
      return 0;
    }

    static int tree(Bach bach, PrintWriter out, PrintWriter err, String... args) {
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

    static class StringPrintWriter extends PrintWriter {
      StringPrintWriter() {
        super(new StringWriter());
      }

      @Override
      public String toString() {
        return super.out.toString();
      }
    }

    static class MirroringStringPrintWriter extends StringPrintWriter {
      private final PrintWriter other;

      MirroringStringPrintWriter(PrintWriter other) {
        this.other = other;
      }

      @Override
      public void flush() {
        super.flush();
        other.flush();
      }

      @Override
      public void write(int c) {
        super.write(c);
        other.write(c);
      }

      @Override
      public void write(char[] buf, int off, int len) {
        super.write(buf, off, len);
        other.write(buf, off, len);
      }

      @Override
      public void write(String s, int off, int len) {
        super.write(s, off, len);
        other.write(s, off, len);
      }

      @Override
      public void println() {
        super.println();
        other.println();
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

  /** Project model and project-related operators. */
  public record Project() {
    static int compile(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      return 0;
    }
  }

  /** A tool reference. */
  public record Tool(Set<Flag> flags, String name, ToolProvider provider) {

    public enum Flag {
      HIDDEN
    }

    public static Tool of(ToolProvider provider) {
      var module = provider.getClass().getModule();
      var name = module.isNamed() ? module.getName() + '/' + provider.name() : provider.name();
      return new Tool(Set.of(), name, provider);
    }

    public static Tool of(String name, Operator operator) {
      record Local(Operator provider) implements Operator {
        @Override
        public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
          return provider.run(bach, out, err, args);
        }
      }
      return new Tool(Set.of(), name, new Local(operator));
    }

    public static Tool ofNativeToolInJavaHome(String name) {
      var executable = Path.of(System.getProperty("java.home"), "bin", name);
      return Tool.of(new NativeToolProvider("java-home/" + name, List.of(executable.toString())));
    }

    public static Tool ofNativeTool(String name, List<String> command) {
      return Tool.of(new NativeToolProvider(name, command));
    }

    public Tool with(Flag flag) {
      var flags = Stream.concat(this.flags.stream(), Stream.of(flag)).toList();
      return new Tool(Set.copyOf(flags), name, provider);
    }

    public boolean isNotHidden() {
      return !flags.contains(Flag.HIDDEN);
    }

    public boolean isNameMatching(String text) {
      // name = "foo/bar" matches text = "foo/bar"
      // name = "foo/bar" matches text = "bar" because name ends with "/bar"
      return name.equals(text) || name.endsWith('/' + text);
    }

    /** A command consisting of a tool name and a list of arguments. */
    public record Call(String name, List<String> arguments) {
      public static Call of(String name, Object... arguments) {
        if (arguments.length == 0) return new Call(name, List.of());
        if (arguments.length == 1) return new Call(name, List.of(arguments[0].toString()));
        return new Call(name, List.of()).with(Stream.of(arguments));
      }

      public Call with(Stream<?> objects) {
        var strings = objects.map(Object::toString);
        return new Call(name, Stream.concat(arguments.stream(), strings).toList());
      }

      public Call with(Object argument) {
        return with(Stream.of(argument));
      }

      public Call with(String key, Object value, Object... values) {
        var call = with(Stream.of(key, value));
        return values.length == 0 ? call : call.with(Stream.of(values));
      }

      public Call withFindFiles(String glob) {
        return withFindFiles(Path.of(""), glob);
      }

      public Call withFindFiles(Path start, String glob) {
        return withFindFiles(start, "glob", glob);
      }

      public Call withFindFiles(Path start, String syntax, String pattern) {
        var syntaxAndPattern = syntax + ':' + pattern;
        var matcher = start.getFileSystem().getPathMatcher(syntaxAndPattern);
        return withFindFiles(start, Integer.MAX_VALUE, matcher);
      }

      public Call withFindFiles(Path start, int maxDepth, PathMatcher matcher) {
        try (var files = Files.find(start, maxDepth, (p, a) -> matcher.matches(p))) {
          return with(files);
        } catch (Exception exception) {
          throw new RuntimeException("Find files failed in: " + start, exception);
        }
      }
    }

    /** An extension of tool provider for running other tools in custom run implementations. */
    @FunctionalInterface
    public interface Operator extends ToolProvider {
      @Override
      default String name() {
        return getClass().getSimpleName();
      }

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

    /**
     * A finder of tools.
     *
     * <p>What {@link java.lang.module.ModuleFinder ModuleFinder} is to {@link
     * java.lang.module.ModuleReference ModuleReference}, is {@link Finder} to {@link Tool}.
     */
    @FunctionalInterface
    public interface Finder {

      List<Tool> findAll();

      default List<Tool> find(String name) {
        return find(name, Tool::isNameMatching);
      }

      default List<Tool> find(String name, BiPredicate<Tool, String> filter) {
        return findAll().stream().filter(tool -> filter.test(tool, name)).toList();
      }

      static Finder of(Tool... tools) {
        record DirectToolFinder(List<Tool> findAll) implements Finder {}
        return new DirectToolFinder(List.of(tools));
      }

      static Finder of(ClassLoader loader) {
        return Finder.of(ServiceLoader.load(ToolProvider.class, loader));
      }

      static Finder of(ServiceLoader<ToolProvider> loader) {
        record ServiceLoaderToolFinder(ServiceLoader<ToolProvider> loader) implements Finder {
          @Override
          public List<Tool> findAll() {
            synchronized (loader) {
              return loader.stream().map(ServiceLoader.Provider::get).map(Tool::of).toList();
            }
          }
        }

        return new ServiceLoaderToolFinder(loader);
      }

      static Finder ofSystemTools() {
        return Finder.of(ClassLoader.getSystemClassLoader());
      }

      static Finder ofBasicTools(Path directory) {
        record BasicToolProvider(String name, Properties properties) implements Operator {
          @Override
          public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
            var verbose = bach.configuration.isVerbose();
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
              var call = Tool.Call.of(name).with(lines.stream().skip(1));
              bach.run(call);
            }
            if (verbose) out.println("END # of " + name);
            return 0;
          }

          private String replace(Bach bach, String line) {
            var root = bach.configuration.paths.root;
            var out = bach.configuration.paths.out;
            return line.trim()
                .replace("{{bach.paths.root}}", Core.PathSupport.normalized(root))
                .replace("{{bach.paths.out}}", Core.PathSupport.normalized(out))
                .replace("{{path.separator}}", System.getProperty("path.separator"));
          }
        }

        record BasicToolFinder(Path directory) implements Finder {
          @Override
          public List<Tool> findAll() {
            if (!Files.isDirectory(directory)) return List.of();
            var list = new ArrayList<Tool>();
            try (var paths = Files.newDirectoryStream(directory, "*.properties")) {
              for (var path : paths) {
                if (Files.isDirectory(path)) continue;
                var namespace = path.getParent().getFileName().toString();
                var filename = path.getFileName().toString();
                var name = filename.substring(0, filename.length() - ".properties".length());
                var properties = Core.PathSupport.properties(path);
                list.add(Tool.of(new BasicToolProvider(namespace + "/" + name, properties)));
              }
            } catch (Exception exception) {
              throw new RuntimeException(exception);
            }
            return List.copyOf(list);
          }
        }

        return new BasicToolFinder(directory);
      }

      static Finder ofJavaTools(Path directory, Path java, String argsfile) {
        record ProgramToolFinder(Path path, Path java, String argsfile) implements Finder {

          @Override
          public List<Tool> findAll() {
            return find(path.normalize().toAbsolutePath().getFileName().toString());
          }

          @Override
          public List<Tool> find(String name) {
            var directory = path.normalize().toAbsolutePath();
            if (!Files.isDirectory(directory)) return List.of();
            var namespace = path.getParent().getFileName().toString();
            if (!name.equals(directory.getFileName().toString())) return List.of();
            var command = new ArrayList<String>();
            command.add(java.toString());
            var args = directory.resolve(argsfile);
            if (Files.isRegularFile(args)) {
              command.add("@" + args);
              return List.of(Tool.ofNativeTool(namespace + '/' + name, command));
            }
            var jars = Core.PathSupport.list(directory, Core.PathSupport::isJarFile);
            if (jars.size() == 1) {
              command.add("-jar");
              command.add(jars.get(0).toString());
              return List.of(Tool.ofNativeTool(namespace + '/' + name, command));
            }
            var javas = Core.PathSupport.list(directory, Core.PathSupport::isJavaFile);
            if (javas.size() == 1) {
              command.add(javas.get(0).toString());
              return List.of(Tool.ofNativeTool(namespace + '/' + name, command));
            }
            throw new UnsupportedOperationException("Unknown program layout: " + directory.toUri());
          }
        }

        record ProgramsToolFinder(Path path, Path java, String argsfile) implements Finder {
          @Override
          public List<Tool> findAll() {
            return Core.PathSupport.list(path, Files::isDirectory).stream()
                .map(directory -> new ProgramToolFinder(directory, java, argsfile))
                .map(Finder::findAll)
                .flatMap(List::stream)
                .toList();
          }
        }
        return new ProgramsToolFinder(directory, java, argsfile);
      }

      static Finder compose(Finder... finders) {
        record CompositeToolFinder(List<Finder> finders) implements Finder {
          @Override
          public List<Tool> findAll() {
            return finders.stream().flatMap(finder -> finder.findAll().stream()).toList();
          }

          @Override
          public List<Tool> find(String name) {
            return finders.stream().flatMap(finder -> finder.find(name).stream()).toList();
          }
        }
        return new CompositeToolFinder(List.of(finders));
      }
    }

    record NativeToolProvider(String name, List<String> command) implements ToolProvider {

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

  /** An exception thrown to indicate that no tool could be found via the given name. */
  public static final class ToolNotFoundException extends RuntimeException {
    @java.io.Serial private static final long serialVersionUID = -417539767734303099L;

    ToolNotFoundException(String name) {
      super("Tool named `%s` not found".formatted(name));
    }
  }

  /** An exception thrown to indicate that multiple tools were found for a given name. */
  public static final class ToolNotUniqueException extends RuntimeException {
    @java.io.Serial private static final long serialVersionUID = -4475718006846080166L;

    ToolNotUniqueException(String name, List<Tool> tools) {
      super(
          """
          Multiple tools found for `%s`:
            - %s
          """
              .formatted(name, tools.stream().map(Tool::name).collect(Collectors.joining("\n  - ")))
              .stripTrailing());
    }
  }
}
