import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.RequiresTree;
import com.sun.source.util.JavacTask;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.SimpleJavaFileObject;
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
    var remnants = configuration.remnants;
    if (remnants.isEmpty()) {
      run(Tool.Call.of("/?"));
      return 1;
    }
    var verbose = configuration.isVerbose();
    var printer = configuration.printer;
    var paths = configuration.paths;
    try (var recording = new Recording()) {
      recording.start();
      try {
        if (verbose) printer.out("BEGIN");
        run(Tool.Call.of(remnants));
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
    var finder = configuration.project.tools.finder;

    var tools = finder.find(name);
    if (tools.isEmpty()) throw new ToolNotFoundException(name);
    if (tools.size() != 1) throw new ToolNotUniqueException(name, tools);
    var tool = tools.get(0);

    var names = stackedToolCallNames.get();
    try {
      names.addLast(name);
      if (tool.isNotHidden()) {
        if (verbose) {
          var thread = Thread.currentThread().getId();
          printer.out("[%2X] %s".formatted(thread, String.join(" | ", names)));
        }
        var text = arguments.isEmpty() ? name : name + ' ' + Core.StringSupport.join(arguments);
        var operator = tool.provider instanceof Tool.Operator;
        printer.out(operator ? text : "  " + text);
      }
      var code = run(tool.provider, name, arguments);
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

  /** A set of annotation interfaces used for project configuration. */
  interface Info {

    Project DEFAULT_PROJECT = Project.class.getAnnotation(Project.class);
    Space DEFAULT_SPACE = Space.class.getAnnotation(Space.class);

    private static <V> V get(Project from, Function<Project, V> with) {
      var value = with.apply(from);
      return value.equals(with.apply(DEFAULT_PROJECT)) ? null : value;
    }

    private static <V> V get(Space from, Function<Space, V> with) {
      var value = with.apply(from);
      return value.equals(with.apply(DEFAULT_SPACE)) ? null : value;
    }

    static Project newProjectAnnotation(Path info) {
      var parent = Bach.class.getClassLoader();
      if ("jdk.jshell".equals(parent.getClass().getModule().getName())) return DEFAULT_PROJECT;
      if (Files.notExists(info)) return DEFAULT_PROJECT;
      try {
        var temp = Files.createTempDirectory("bach-");
        var name = "Project4711";
        var lines =
            Files.readAllLines(info).stream()
                .map(line -> line.replace("@Project", "@Bach.Info.Project"))
                .map(line -> line.replace("@Space", "@Bach.Info.Space"))
                .map(line -> line.replace("@Tools", "@Bach.Info.Tools"))
                .map(line -> line.replace("@Script", "@Bach.Info.Tools.Script"))
                .map(line -> line.replace("@ExternalTool", "@Bach.Info.Tools.ExternalTool"))
                .map(line -> line.replace("@Asset", "@Bach.Info.Asset"))
                .toList();
        var hook = Files.write(temp.resolve(name + ".java"), lines);
        Files.writeString(hook, "interface " + name + " {}", StandardOpenOption.APPEND);

        var javac = Tool.Call.of("javac").with("-d", temp).with(hook);

        var sourcefile =
            System.getProperty("bach.sourcefile", System.getProperty("jdk.launcher.sourcefile"));
        if (sourcefile != null) javac = javac.with(sourcefile);
        var codesource = Bach.class.getProtectionDomain().getCodeSource();
        if (codesource != null) {
          var codelocation = codesource.getLocation();
          if (codelocation != null) {
            var codepath = Path.of(codelocation.toURI());
            javac = javac.with("--class-path", codepath);
          }
        }

        var code =
            ToolProvider.findFirst("javac")
                .orElseThrow()
                .run(System.out, System.err, javac.arguments().toArray(String[]::new));
        if (code != 0) {
          throw new RuntimeException("Compiling " + info + " failed");
        }
        var urls = new URL[] {temp.toUri().toURL()};
        try (var loader = URLClassLoader.newInstance(urls, parent)) {
          return loader.loadClass(name).getAnnotation(Project.class);
        }
      } catch (Exception exception) {
        throw new RuntimeException("Creating @Project from " + info + " failed", exception);
      }
    }

    @Project
    @Retention(RetentionPolicy.RUNTIME)
    @interface Project {
      String name() default "unnamed";

      String version() default "0-ea";

      String versionDate() default "";

      Space init() default @Space();

      Space main() default @Space();

      Space test() default @Space();

      Tools tools() default @Tools();
    }

    @Space
    @Retention(RetentionPolicy.RUNTIME)
    @interface Space {
      String[] modules() default {};

      String launcher() default "";

      int release() default 0;
    }

    @Tools
    @Retention(RetentionPolicy.RUNTIME)
    @interface Tools {
      ExternalTool[] externals() default {};

      Script[] scripts() default {};

      @Retention(RetentionPolicy.RUNTIME)
      @interface ExternalTool {
        String name();

        Asset[] assets() default {};
      }

      @Retention(RetentionPolicy.RUNTIME)
      @interface Script {
        String name();

        String[] code() default {};
      }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Asset {
      String name();

      String from();
    }
  }

  /** Immutable settings. */
  public record Configuration(
      Printer printer, Flags flags, Paths paths, Project project, List<String> remnants) {
    enum Flag {
      VERBOSE
    }

    public static Configuration of(PrintWriter out, PrintWriter err, String... args) {
      var printer = new Printer(out, err);
      var flags = EnumSet.noneOf(Flag.class);
      var root = Path.of("");
      var bout = Path.of(".bach", "out");
      var info = Path.of("project-info.java");
      var projectProperties = new TreeMap<String, String>();

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
          if (key.equals("--change-bach-out")) {
            bout = Path.of(value).normalize();
            continue;
          }
          if (key.equals("--chinfo")) {
            info = Path.of(value).normalize();
            continue;
          }
          if (key.startsWith("--project-")) {
            projectProperties.put(key, value);
            continue;
          }
          throw new IllegalArgumentException("Unsupported option `%s`".formatted(key));
        }
        arguments.addFirst(argument); // restore `TOOL-NAME` argument
        break;
      }
      var remnants = List.copyOf(arguments);
      var annotation = Info.newProjectAnnotation(root.resolve(info));
      var paths = new Paths(root, root.resolve(bout));
      var tools = Project.Tools.of(paths, annotation);
      var project =
          Project.of()
              .with(tools)
              .withParsingDirectory(root)
              .withParsingAnnotation(root, annotation)
              .withParsingProperties(projectProperties);

      return new Configuration(printer, new Flags(Set.copyOf(flags)), paths, project, remnants);
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
        bach.configuration.project.tools.finder.findAll().stream()
            .filter(Tool::isNotHidden)
            .sorted(Comparator.comparing(Tool::name))
            .forEach(tool -> out.printf("  - %s%n", tool.name()));
      }
      return 0;
    }

    static int load(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      record CLI(Optional<Boolean> reload, Path target, String source) {
        static final String USAGE =
            """
            Usage: load [--reload] TARGET SOURCE
            """;

        static Optional<CLI> of(String... args) {
          if (args.length < 2 || args.length > 3) return Optional.empty();
          var reload = "--reload".equals(args[0]);
          return Optional.of(
              new CLI(
                  reload ? Optional.of(Boolean.TRUE) : Optional.empty(),
                  Path.of(args[reload ? 1 : 0]),
                  args[reload ? 2 : 1]));
        }
      }
      var optionalCLI = CLI.of(args);
      if (optionalCLI.isEmpty()) {
        err.println(CLI.USAGE);
        return 1;
      }
      var cli = optionalCLI.get();
      var reload = cli.reload().orElse(Boolean.FALSE);
      var target = cli.target();
      if (Files.exists(target) && !reload) return 0;

      var source = cli.source();
      if (source.startsWith("string:")) {
        try {
          var string = source.substring(7);
          Files.writeString(target, string);
          out.printf("Wrote %,12d %s%n", string.length(), target.getFileName());
          return 0;
        } catch (Exception exception) {
          exception.printStackTrace(err);
          return 2;
        }
      }

      var paths = bach.configuration.paths;
      var from = URI.create(source);
      var scheme = from.getScheme();
      var uri = scheme == null ? paths.root(source).normalize().toAbsolutePath().toUri() : from;
      out.printf("Loading %s...%n", uri);
      try (var stream = uri.toURL().openStream()) {
        var parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        var size = Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        out.printf("Loaded %,12d %s%n", size, target.getFileName());
        return 0;
      } catch (Exception exception) {
        exception.printStackTrace(err);
        return 3;
      }
    }

    static int loadAndVerify(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      bach.run("load", args[0], args[1]);

      var target = Path.of(args[0]);
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

      if (!args[1].startsWith("string:")) {
        var fragment = URI.create(args[1]).getFragment();
        var elements = fragment == null ? new String[0] : fragment.split("&");
        for (var element : elements) checker.accept(element);
      }

      if (calls.isEmpty()) throw new IllegalStateException("No expected checksum given.");

      calls.stream().parallel().forEach(bach::run);
      return 0;
    }

    static int info(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      out.println("Configuration");
      out.println(bach.configuration.flags);
      out.println(bach.configuration.paths);
      out.println("Project");
      out.println(bach.configuration.project.name);
      out.println(bach.configuration.project.version);
      out.println("Tools");
      bach.configuration.project.tools.finder.findAll().stream()
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
      bach.run("load", "--reload", target, from);
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
        if (mode == Mode.CREATE || mode == Mode.CLEAN) {
          Files.createDirectories(path);
        }
        if (mode == Mode.DELETE || mode == Mode.CLEAN) {
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

    /** Static utility methods for operating on instances of {@link ModuleDescriptor}. */
    static class ModuleDescriptorSupport {

      /**
       * Reads the source form of a module declaration from a file as a module descriptor.
       *
       * @param info the path to a {@code module-info.java} file to parse
       * @return the module descriptor
       * @implNote For the time being, only the {@code kind}, the {@code name} and its {@code
       *     requires} directives are parsed.
       */
      public static ModuleDescriptor parse(Path info) {
        if (!Path.of("module-info.java").equals(info.getFileName()))
          throw new IllegalArgumentException("Path must end with 'module-info.java': " + info);

        var compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        var writer = new PrintWriter(Writer.nullWriter());
        var fileManager = compiler.getStandardFileManager(null, null, null);
        var units = List.of(new ModuleInfoFileObject(info));
        var javacTask = (JavacTask) compiler.getTask(writer, fileManager, null, null, null, units);

        try {
          for (var tree : javacTask.parse()) {
            var module = tree.getModule();
            if (module == null) throw new AssertionError("No module tree?! -> " + info);
            return parse(module);
          }
        } catch (IOException e) {
          throw new UncheckedIOException("Parse failed for " + info, e);
        }
        throw new IllegalArgumentException("Module tree not found in " + info);
      }

      private static ModuleDescriptor parse(ModuleTree moduleTree) {
        var moduleName = moduleTree.getName().toString();
        var moduleModifiers =
            moduleTree.getModuleType().equals(ModuleTree.ModuleKind.OPEN)
                ? EnumSet.of(ModuleDescriptor.Modifier.OPEN)
                : EnumSet.noneOf(ModuleDescriptor.Modifier.class);
        var moduleBuilder = ModuleDescriptor.newModule(moduleName, moduleModifiers);
        for (var directive : moduleTree.getDirectives()) {
          if (directive instanceof RequiresTree requires) {
            var requiresModuleName = requires.getModuleName().toString();
            moduleBuilder.requires(requiresModuleName);
          }
        }
        return moduleBuilder.build();
      }

      static class ModuleInfoFileObject extends SimpleJavaFileObject {
        ModuleInfoFileObject(Path path) {
          super(path.toUri(), Kind.SOURCE);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
          return Files.readString(Path.of(uri));
        }
      }
    }

    static class ModuleSourcePathSupport {

      public static List<String> compute(Map<String, List<Path>> map, boolean forceSpecificForm) {
        var patterns = new TreeSet<String>(); // "src:etc/*/java"
        var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
        for (var entry : map.entrySet()) {
          var name = entry.getKey();
          var paths = entry.getValue();
          if (forceSpecificForm) {
            specific.put(name, paths);
            continue;
          }
          try {
            for (var path : paths) {
              patterns.add(toPatternForm(path, name));
            }
          } catch (FindException e) {
            specific.put(name, paths);
          }
        }
        return Stream.concat(
                patterns.stream(),
                specific.entrySet().stream().map(e -> toSpecificForm(e.getKey(), e.getValue())))
            .toList();
      }

      public static String toPatternForm(Path info, String module) {
        var root = info.getRoot();
        var deque = new ArrayDeque<String>();
        if (root != null) deque.add(root.toString());
        for (var element : info.normalize()) {
          var name = element.toString();
          if (name.equals("module-info.java")) continue;
          deque.addLast(name.equals(module) ? "*" : name);
        }
        var pattern = String.join(File.separator, deque);
        if (!pattern.contains("*"))
          throw new FindException("Name '" + module + "' not found: " + info);
        if (pattern.equals("*")) return ".";
        if (pattern.endsWith("*")) return pattern.substring(0, pattern.length() - 2);
        if (pattern.startsWith("*")) return "." + File.separator + pattern;
        return pattern;
      }

      public static String toSpecificForm(String module, List<Path> paths) {
        return module
            + '='
            + paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
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

      static boolean isJarFile(Path path) {
        return nameOrElse(path, "").endsWith(".jar") && Files.isRegularFile(path);
      }

      static boolean isJavaFile(Path path) {
        return nameOrElse(path, "").endsWith(".java") && Files.isRegularFile(path);
      }

      static boolean isModuleInfoJavaFile(Path path, BasicFileAttributes... attributes) {
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
      static String join(List<String> strings) {
        return strings.stream().map(StringSupport::firstLine).collect(Collectors.joining(" "));
      }

      static String firstLine(String string) {
        var lines = string.lines().toList();
        var size = lines.size();
        return size <= 1 ? string : lines.get(0) + "[...%d lines]".formatted(size);
      }

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
  public record Project(Name name, Version version, Spaces spaces, Tools tools) {

    public static Project of() {
      var init = new Space("init");
      var main = new Space("main", "init");
      var test = new Space("test", "main");
      return new Project(
          new Name(Info.DEFAULT_PROJECT.name()),
          new Version(Info.DEFAULT_PROJECT.version(), ZonedDateTime.now()),
          new Spaces(init, main, test),
          new Tools(Tool.Finder.of(), List.of()));
    }

    public Project withParsingAnnotation(Path root, Info.Project info) {
      var project = this;
      var name = Info.get(info, Info.Project::name);
      project = name == null ? project : project.with(new Name(name));
      var version = Info.get(info, Info.Project::version);
      project = version == null ? project : project.with(project.version.with(version));
      var date = Info.get(info, Info.Project::versionDate);
      project = date == null ? project : project.with(project.version.withDate(date));
      project = project.with(project.spaces.init.withParsing(root, info.init()));
      project = project.with(project.spaces.main.withParsing(root, info.main()));
      project = project.with(project.spaces.test.withParsing(root, info.test()));
      return project;
    }

    public Project withParsingDirectory(Path directory) {
      var project = this;
      var name = directory.normalize().toAbsolutePath().getFileName();
      if (name != null) project = project.with(new Name(name.toString()));
      try (var stream = Files.find(directory, 9, Core.PathSupport::isModuleInfoJavaFile)) {
        var inits = new ArrayList<DeclaredModule>();
        var mains = new ArrayList<DeclaredModule>();
        var tests = new ArrayList<DeclaredModule>();
        for (var path : stream.toList()) {
          var uri = path.toUri().toString();
          var list = uri.contains("/init/") ? inits : uri.contains("/test/") ? tests : mains;
          var module = DeclaredModule.of(path);
          list.add(module);
        }
        project = project.with(project.spaces.init.withModules(List.copyOf(inits)));
        project = project.with(project.spaces.main.withModules(List.copyOf(mains)));
        project = project.with(project.spaces.test.withModules(List.copyOf(tests)));
      } catch (Exception exception) {
        throw new RuntimeException("Find module-info.java files failed", exception);
      }
      return project;
    }

    public Project withParsingProperties(Map<String, String> map) {
      var project = this;
      var name = map.get("--project-name");
      project = name == null ? project : project.with(new Name(name));
      var version = map.get("--project-version");
      project = version == null ? project : project.with(project.version.with(version));
      var date = map.get("--project-version-date");
      project = date == null ? project : project.with(project.version.withDate(date));
      return project;
    }

    private Project with(Component component) {
      return new Project(
          component instanceof Name name ? name : name,
          component instanceof Version version ? version : version,
          component instanceof Spaces spaces ? spaces : spaces,
          component instanceof Tools tools ? tools : tools);
    }

    private Project with(Space space) {
      return with(spaces.with(space));
    }

    private sealed interface Component {}

    public record Name(String value) implements Component {
      public Name {
        Objects.requireNonNull(value);
      }

      @Override
      public String toString() {
        return value;
      }
    }

    public record Version(String value, ZonedDateTime date) implements Component {
      public Version {
        ModuleDescriptor.Version.parse(value);
      }

      Version with(String value) {
        return new Version(value, date);
      }

      Version with(ZonedDateTime date) {
        return new Version(value, date);
      }

      Version withDate(String text) {
        return with(ZonedDateTime.parse(text));
      }
    }

    public record Spaces(Space init, Space main, Space test) implements Component {
      List<Space> list() {
        return List.of(init, main, test);
      }

      Spaces with(Space space) {
        return new Spaces(
            space.name.equals(init.name) ? space : init,
            space.name.equals(main.name) ? space : main,
            space.name.equals(test.name) ? space : test);
      }
    }

    public record Asset(String name, String from) {
      public static Asset of(Info.Asset info) {
        return new Asset(info.name(), info.from());
      }
    }

    public record ExternalTool(String name, List<Asset> assets) {
      public static ExternalTool of(Info.Tools.ExternalTool info) {
        return new ExternalTool(info.name(), Stream.of(info.assets()).map(Asset::of).toList());
      }
    }

    public record Tools(Tool.Finder finder, List<ExternalTool> externals) implements Component {
      public static Tools of(Configuration.Paths paths, Info.Project info) {
        var finder =
            Tool.Finder.compose(
                Tool.Finder.of(
                    Tool.of("bach/help", Core::help),
                    Tool.of("/?", Core::help),
                    Tool.of("/save", Core::save)),
                Tool.Finder.ofProjectScripts(info.tools()),
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
                Tool.Finder.of(
                    Tool.of("project/build", Bach.Project::build),
                    Tool.of("project/download", Bach.Project::download),
                    Tool.of("project/compile", Bach.Project::compile),
                    Tool.of("project/launch", Bach.Project::launch)),
                Tool.Finder.ofSystemTools(),
                Tool.Finder.of(
                    Tool.ofNativeToolInJavaHome("jarsigner"),
                    Tool.ofNativeToolInJavaHome("java").with(Tool.Flag.HIDDEN),
                    Tool.ofNativeToolInJavaHome("jdeprscan"),
                    Tool.ofNativeToolInJavaHome("jfr")));

        var externals = Stream.of(info.tools().externals()).map(ExternalTool::of).toList();
        return new Tools(finder, externals);
      }
    }

    public record Space(
        String name,
        List<DeclaredModule> modules,
        int release,
        Optional<String> launcher,
        List<String> requires, // used to compute "--[processor-]module-path"
        List<String> additionalCompileJavacArguments) {
      public Space {
        Objects.requireNonNull(name);
        Objects.requireNonNull(modules);
        var feature = Runtime.version().feature();
        if (release != 0 && (release < 9 || release > feature))
          throw new IndexOutOfBoundsException(
              "Release %d not in range of %d..%d".formatted(release, 9, feature));
        Objects.requireNonNull(launcher);
        Objects.requireNonNull(requires);
      }

      public Space(String name, String... requires) {
        this(
            name,
            List.of(),
            Info.DEFAULT_SPACE.release(),
            Optional.empty(),
            List.of(requires),
            List.of());
      }

      public Space withModules(List<DeclaredModule> modules) {
        return new Space(
            name, modules, release, launcher, requires, additionalCompileJavacArguments);
      }

      public Space withParsing(Path root, Info.Space space) {
        var modules = Info.get(space, Info.Space::modules);
        var release = Info.get(space, Info.Space::release);
        var launcher = Info.get(space, Info.Space::launcher);
        // TODO var requires = value(space, ModuleSpace::requires)
        // TODO var additionalCompileJavacArguments = value(space, ModuleSpace::...)
        return new Space(
            name,
            modules == null
                ? this.modules
                : Stream.of(modules).map(root::resolve).map(DeclaredModule::of).toList(),
            release == null ? this.release : release,
            launcher == null ? this.launcher : Optional.of(launcher),
            requires,
            additionalCompileJavacArguments);
      }

      public Optional<Integer> targets() {
        return release == 0 ? Optional.empty() : Optional.of(release);
      }
    }

    public record DeclaredModule(Path info, ModuleDescriptor descriptor, Folders folders) {
      public static DeclaredModule of(Path path) {
        var info = path.endsWith("module-info.java") ? path : path.resolve("module-info.java");
        var descriptor = Core.ModuleDescriptorSupport.parse(info.normalize());
        var folders = Folders.of(info);
        return new DeclaredModule(info, descriptor, folders);
      }

      public String toName() {
        return descriptor.name();
      }

      public List<Path> toModuleSourcePaths() {
        var parent = info.getParent();
        return parent != null ? List.of(parent) : List.of(Path.of("."));
      }
    }

    public record Folders(List<Folder> list) {
      public static Folders of(Path info) {
        var parent = info.getParent();
        if (parent == null) return new Folders(List.of());
        if (!parent.getFileName().toString().startsWith("java"))
          return new Folders(List.of(Folder.of(parent)));
        var javas = parent.getParent();
        if (javas == null) return new Folders(List.of(Folder.of(parent)));
        try (var stream = Files.list(javas)) {
          return new Folders(
              stream
                  .filter(Files::isDirectory)
                  .filter(path -> path.getFileName().toString().startsWith("java"))
                  .map(Folder::of)
                  .sorted()
                  .toList());
        } catch (Exception exception) {
          throw new RuntimeException("Listing entries of %s failed".formatted(info));
        }
      }
    }

    public record Folder(Path path, int release) implements Comparable<Folder> {
      static final Pattern RELEASE_PATTERN = Pattern.compile(".*?(\\d+)$");

      static int parseReleaseNumber(String string) {
        var matcher = RELEASE_PATTERN.matcher(string);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
      }

      public static Folder of(Path path) {
        if (Files.notExists(path)) throw new IllegalArgumentException("No such path: " + path);
        if (Files.isRegularFile(path))
          throw new IllegalArgumentException("Not a directory: " + path);
        return new Folder(path, parseReleaseNumber(path.getFileName().toString()));
      }

      @Override
      public int compareTo(Folder other) {
        return release - other.release;
      }
    }

    static int build(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      bach.run("project/download");
      bach.run("project/compile");
      return 0;
    }

    static int download(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      for (var external : bach.configuration.project.tools.externals) {
        var directory = bach.configuration.paths.root(".bach", "external-tools", external.name);
        for (var asset : external.assets) {
          var target = directory.resolve(asset.name);
          var source = asset.from;
          var call = Tool.Call.of("load-and-verify", target, source);
          if (source.startsWith("string:")) {
            var size = source.getBytes(StandardCharsets.UTF_8).length - 7;
            call = call.with("SIZE=" + size);
          }
          bach.run(call);
        }
      }
      return 0;
    }

    static int compile(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      var project = bach.configuration.project;
      var paths = bach.configuration.paths;

      for (var space : project.spaces.list()) {
        var declarations = space.modules;
        if (declarations.isEmpty()) {
          out.println("No %s modules declared.".formatted(space.name()));
          continue;
        }
        out.println(
            "Compile and package %d %s module%s..."
                .formatted(declarations.size(), space.name(), declarations.size() == 1 ? "" : "s"));

        var classes = paths.out(space.name(), "classes");
        var modules = paths.out(space.name(), "modules");

        var javac = Tool.Call.of("javac");

        var release0 = space.targets();
        if (release0.isPresent()) {
          javac = javac.with("--release", release0.get());
        }

        javac =
            javac.with(
                "--module",
                declarations.stream()
                    .map(Project.DeclaredModule::descriptor)
                    .map(ModuleDescriptor::name)
                    .collect(Collectors.joining(",")));
        var map =
            declarations.stream()
                .collect(
                    Collectors.toMap(
                        Project.DeclaredModule::toName,
                        Project.DeclaredModule::toModuleSourcePaths));
        for (var moduleSourcePath : Core.ModuleSourcePathSupport.compute(map, false)) {
          javac = javac.with("--module-source-path", moduleSourcePath);
        }

        var modulePath =
            space.requires().stream()
                .map(required -> paths.out(required, "modules"))
                .filter(Files::isDirectory)
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
        if (!modulePath.isEmpty()) {
          javac = javac.with("--module-path", modulePath);
          javac = javac.with("--processor-module-path", modulePath);
        }

        var classes0 = classes.resolve("java-" + release0.orElse(Runtime.version().feature()));
        javac = javac.with("-d", classes0);

        for (var additionalCompileJavacArgument : space.additionalCompileJavacArguments) {
          javac = javac.with(additionalCompileJavacArgument);
        }

        bach.run(javac);

        try {
          Files.createDirectories(modules);
        } catch (Exception exception) {
          throw new RuntimeException("Create directories failed: " + modules);
        }

        var javacCommands = new ArrayList<Tool.Call>();
        var jarCommands = new ArrayList<Tool.Call>();
        var jarFiles = new ArrayList<Path>();
        for (var module : declarations) {
          var name = module.descriptor().name();
          var file = modules.resolve(name + ".jar");

          jarFiles.add(file);

          var jar = Tool.Call.of("jar").with("--create").with("--file", file);

          jar = jar.with("--module-version", project.version().value());

          if (Runtime.version().feature() >= 19) {
            var date = project.version().date();
            jar = jar.with("--date", date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
          }

          var mainProgram = name.replace('.', '/') + "/Main.java";
          var mainJava = module.toModuleSourcePaths().get(0).resolve(mainProgram);
          if (Files.exists(mainJava)) {
            jar = jar.with("--main-class", name + ".Main");
          }

          jar = jar.with("-C", classes0.resolve(name), ".");

          for (var folder : module.folders().list().stream().skip(1).toList()) {
            var release = folder.release();
            var classesR = classes.resolve("java-" + release).resolve(name);
            javacCommands.add(
                Tool.Call.of("javac")
                    .with("--release", release)
                    .with("-d", classesR)
                    .withFindFiles(folder.path(), "**.java"));
            jar = jar.with("--release", release).with("-C", classesR, ".");
          }

          jarCommands.add(jar);
        }
        javacCommands.stream().parallel().forEach(bach::run);
        jarCommands.stream().parallel().forEach(bach::run);

        jarFiles.forEach(
            file -> {
              if (Files.notExists(file)) {
                out.println("JAR file not found: " + file);
                return;
              }
              var size = Core.PathSupport.computeChecksum(file, "SIZE");
              var hash = Core.PathSupport.computeChecksum(file, "SHA-256");
              out.println("%s %11s %s".formatted(hash, size, file));
            });
      }
      return 0;
    }

    static int launch(Bach bach, PrintWriter out, PrintWriter err, String... args) {
      var launcher = bach.configuration.project.spaces().main().launcher();
      var paths = bach.configuration.paths;
      if (launcher.isEmpty()) {
        err.println("No launcher defined. No start.");
        return 1;
      }

      var java =
          Tool.Call.of("java")
              .with("--module-path", paths.out("main", "modules"))
              .with("--module", launcher.get())
              .with(Stream.of(args));
      bach.run(java);
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

      public static Call of(List<?> command) {
        var size = command.size();
        if (size == 0) throw new IllegalArgumentException("Empty command");
        var name = command.get(0).toString();
        if (size == 1) return new Call(name, List.of());
        if (size == 2) return new Call(name, List.of(command.get(1).toString()));
        return new Call(name, List.of()).with(command.stream().skip(1));
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

      static Finder ofProjectScripts(Info.Tools tools) {
        record ProjectScriptTool(String name, String[] values) implements Operator {
          static ProjectScriptTool of(Info.Tools.Script script) {
            return new ProjectScriptTool("project-script/" + script.name(), script.code());
          }

          @Override
          public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
            var verbose = bach.configuration.isVerbose();
            if (verbose) out.println("BEGIN # of " + name);
            for (var value : values) {
              var lines = value.lines().map(line -> replace(bach, line)).toList();
              bach.run(Tool.Call.of(lines));
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

        record ProjectScriptFinder(List<Info.Tools.Script> scripts) implements Finder {

          @Override
          public List<Tool> findAll() {
            return scripts.stream().map(ProjectScriptTool::of).map(Tool::of).toList();
          }
        }

        return new ProjectScriptFinder(List.of(tools.scripts()));
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
            return List.of();
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
