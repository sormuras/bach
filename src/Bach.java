import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.StackTrace;

public record Bach(Options options, Logbook logbook, Paths paths, Tools tools) {

  public static void main(String... args) {
    var bach = Bach.instance(args);
    var code = bach.main();
    System.exit(code);
  }

  private static final AtomicReference<Bach> INSTANCE = new AtomicReference<>();

  public static Bach instance(String... args) {
    return instance(() -> Bach.of(args));
  }

  public static Bach instance(Consumer<String> out, Consumer<String> err, String... args) {
    return instance(() -> Bach.of(out, err, args));
  }

  public static Bach instance(Supplier<Bach> supplier) {
    var oldInstance = INSTANCE.get();
    if (oldInstance != null) return oldInstance;
    var newInstance = supplier.get();
    if (INSTANCE.compareAndSet(null, newInstance)) return newInstance;
    return INSTANCE.get();
  }

  public static Bach of(String... args) {
    return Bach.of(System.out::println, System.err::println, args);
  }

  public static Bach of(Consumer<String> out, Consumer<String> err, String... args) {
    var options = Options.of(args);
    var logbook = Logbook.of(out, err, options);
    var paths = Paths.of(options);
    var tools = Tools.of(options);
    return new Bach(options, logbook, paths, tools);
  }

  public void build() {
    run(Command.of("info"));
  }

  public void info() {
    var out = logbook.out();
    out.accept("bach.paths = %s".formatted(paths));
    tools
        .finder()
        .visit(
            0,
            (depth, finder) -> {
              var indent = "  ".repeat(depth);
              out.accept(indent + finder.title());
              if (depth == 0) return;
              finder.findAll().stream()
                  .sorted(Comparator.comparing(ToolProvider::name))
                  .forEach(tool -> out.accept(indent + "  - " + tool.name()));
            });

    Stream.of(
            Command.of("jar").with("--version"),
            Command.of("javac").with("--version"),
            Command.of("javadoc").with("--version"))
        .sequential()
        .forEach(command -> run(command, true));

    Stream.of(
            Command.of("jdeps").with("--version"),
            Command.of("jlink").with("--version"),
            Command.of("jmod").with("--vers5ion"),
            Command.of("jpackage").with("--version"))
        .parallel()
        .forEach(command -> run(command, true));
  }

  int main() {
    try (var recording = new Recording()) {
      recording.start();
      logbook.log(Level.DEBUG, "BEGIN");
      options.commands().forEach(this::run);
      logbook.log(Level.DEBUG, "END.");
      recording.stop();
      var jfr = Files.createDirectories(paths.out()).resolve("bach-logbook.jfr");
      recording.dump(jfr);
      var logfile = paths.out().resolve("bach-logbook.md");
      logbook.out.accept("-> %s".formatted(jfr.toUri()));
      logbook.out.accept("-> %s".formatted(logfile.toUri()));
      var duration = Duration.between(recording.getStartTime(), recording.getStopTime());
      logbook.out.accept("Run took %d.%02d seconds".formatted(duration.toSeconds(), duration.toMillis()));
      Files.write(logfile, logbook.toMarkdownLines());
      return 0;
    } catch (Exception exception) {
      logbook.log(Level.ERROR, exception.toString());
      return -1;
    }
  }

  public void run(Command command) {
    run(command, false); // TODO Bach.Flag.VERBOSE
  }

  public void run(Command command, boolean verbose) {
    /*Logging*/ {
      var args = String.join(" ", command.arguments());
      var line = new StringJoiner(" ");
      line.add(command.name());
      if (!args.isEmpty()) {
        var arguments = args.length() <= 50 ? args : args.substring(0, 45) + "[...]";
        line.add(arguments);
      }
      logbook.log(Level.INFO, line.toString());
    }

    var start = Instant.now();
    var event = tools.run(command);

    if (verbose) {
      if (!event.out.isEmpty()) logbook.out().accept(event.out.indent(2).stripTrailing());
      if (!event.err.isEmpty()) logbook.err().accept(event.err.indent(2).stripTrailing());
      var duration = Duration.between(start, Instant.now());
      var line =
          "%s ran %d.%02d seconds and returned code %d"
              .formatted(command.name(), duration.toSeconds(), duration.toMillis(), event.code);
      var printer = event.code == 0 ? logbook.out() : logbook().err();
      printer.accept(line);
    }
  }

  public record Command(String name, List<String> arguments) {
    public static Command of(String name, Object... arguments) {
      if (arguments.length == 0) return new Command(name, List.of());
      if (arguments.length == 1) return new Command(name, List.of(arguments[0].toString()));
      return new Command(name, List.of()).with(Stream.of(arguments));
    }

    public Command with(Stream<?> objects) {
      var strings = objects.map(Object::toString);
      return new Command(name, Stream.concat(arguments.stream(), strings).toList());
    }

    public Command with(Object argument) {
      return with(Stream.of(argument));
    }

    public Command with(String key, Object value, Object... values) {
      var command = with(Stream.of(key, value));
      return values.length == 0 ? command : command.with(Stream.of(values));
    }

    public Command withFindFiles(String glob) {
      return withFindFiles(Path.of(""), glob);
    }

    public Command withFindFiles(Path start, String glob) {
      return withFindFiles(start, "glob", glob);
    }

    public Command withFindFiles(Path start, String syntax, String pattern) {
      var syntaxAndPattern = syntax + ':' + pattern;
      var matcher = start.getFileSystem().getPathMatcher(syntaxAndPattern);
      return withFindFiles(start, Integer.MAX_VALUE, matcher);
    }

    public Command withFindFiles(Path start, int maxDepth, PathMatcher matcher) {
      try (var files = Files.find(start, maxDepth, (p, a) -> matcher.matches(p))) {
        return with(files);
      } catch (Exception exception) {
        throw new RuntimeException("Find files failed in: " + start, exception);
      }
    }
  }

  public record Paths(Path root, Path out) {
    public static Paths of(Options options) {
      return new Paths(options.__chroot(), options.__destination());
    }
  }

  public record Tools(ToolFinder finder) {
    public static Tools of(Options options) {
      return new Tools(
          ToolFinder.compose(
              ToolFinder.of(
                  new ToolFinder.Provider("build", Tools::build),
                  new ToolFinder.Provider("info", Tools::info)),
              ToolFinder.ofSystem()));
    }

    static int build(PrintWriter out, PrintWriter err, String... args) {
      Bach.instance(out::println, err::println).build();
      return 0;
    }

    static int info(PrintWriter out, PrintWriter err, String... args) {
      Bach.instance(out::println, err::println).info();
      return 0;
    }

    public RunEvent run(Command command) {
      var tool = finder.find(command.name()).orElseThrow();
      var out = new StringWriter();
      var err = new StringWriter();
      var args = command.arguments().toArray(String[]::new);
      var event = new RunEvent();
      event.name = command.name();
      event.args = String.join(" ", command.arguments());
      event.begin();
      event.code = tool.run(new PrintWriter(out), new PrintWriter(err), args);
      event.end();
      event.out = out.toString().strip();
      event.err = err.toString().strip();
      event.commit();
      return event;
    }
  }

  public record Options(
      Level __logbook_threshold, Path __chroot, Path __destination, List<Command> commands) {
    static Options of(String... args) {
      var map = new TreeMap<String, String>();
      var arguments = new ArrayDeque<>(List.of(args));
      var commands = new ArrayList<Command>();
      while (!arguments.isEmpty()) {
        var argument = arguments.removeFirst();
        if (argument.startsWith("--")) {
          var delimiter = argument.indexOf('=', 2);
          var key = delimiter == -1 ? argument : argument.substring(0, delimiter);
          var value = delimiter == -1 ? arguments.removeFirst() : argument.substring(delimiter + 1);
          map.put(key, value);
          continue;
        }
        commands.add(new Command(argument, arguments.stream().toList()));
        break;
      }
      return Options.of(map, commands);
    }

    static Options of(Map<String, String> map, List<Command> commands) {
      var root = Path.of(map.getOrDefault("--chroot", "")).normalize();
      return new Options(
          Level.valueOf(map.getOrDefault("--logbook-threshold", "INFO")),
          root,
          root.resolve(Path.of(map.getOrDefault("--destination", ".bach/out"))).normalize(),
          List.copyOf(commands));
    }
  }

  public record Logbook(
      Consumer<String> out, Consumer<String> err, Level threshold, Deque<LogEvent> logs) {

    public static Logbook of(Consumer<String> out, Consumer<String> err, Options options) {
      return new Logbook(out, err, options.__logbook_threshold(), new ConcurrentLinkedDeque<>());
    }

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

    default void visit(int depth, BiConsumer<Integer, ToolFinder> visitor) {
      visitor.accept(depth, this);
    }

    static ToolFinder compose(ToolFinder... finders) {
      record CompositeToolFinder(List<ToolFinder> finders) implements ToolFinder {
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
      return new CompositeToolFinder(List.of(finders));
    }

    static ToolFinder of(ToolProvider... providers) {
      record DirectToolFinder(List<ToolProvider> findAll) implements ToolFinder {}
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
  }

  @Category("Bach")
  @Name("Bach.LogEvent")
  @Label("Log")
  @StackTrace(false)
  private static final class LogEvent extends Event {
    String level;
    String message;
  }

  @Category("Bach")
  @Name("Bach.RunEvent")
  @Label("Run")
  @StackTrace(false)
  private static final class RunEvent extends Event {
    String name;
    String args;
    int code;
    String out;
    String err;
  }
}
