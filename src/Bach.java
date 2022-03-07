import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.StackTrace;
import jdk.jfr.consumer.RecordingFile;

public record Bach(Options options, Logbook logbook, Paths paths) {

  public static void main(String... args) {
    var bach = Bach.of(System.out::println, System.err::println, args);
    var code = bach.run();
    System.exit(code);
  }

  public static Bach of(Consumer<String> out, Consumer<String> err, String... args) {
    var options = Options.of(args);
    var logbook = Logbook.of(out, err, options);
    var paths = Paths.of(options);
    return new Bach(options, logbook, paths);
  }

  public int run() {
    try (var recording = new Recording()) {
      recording.start();

      logbook.log(Level.INFO, "Hello %s!".formatted(System.getProperty("user.name")));

      recording.stop();
      var jfr = Files.createDirectories(paths.out()).resolve("bach-run.jfr");
      recording.dump(jfr);
      logbook.toMarkdownLines(jfr).forEach(logbook().out());
      return 0;
    } catch (Exception exception) {
      logbook.log(Level.ERROR, exception.toString());
      return -1;
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

  public record Options(Level __logbook_threshold, Path __chroot, Path __destination, List<Command> commands) {
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

  public record Logbook(Consumer<String> out, Consumer<String> err, Level threshold) {
    public static Logbook of(Consumer<String> out, Consumer<String> err, Options options) {
      return new Logbook(out, err, options.__logbook_threshold());
    }

    public void log(Level level, String message) {
      new LogEvent(level, message).commit();
      if (level.getSeverity() < threshold.getSeverity()) return;
      var consumer = level.getSeverity() <= Level.INFO.getSeverity() ? out : err;
      consumer.accept(message);
    }

    public List<String> toMarkdownLines(Path file) {
      try {
        var events = RecordingFile.readAllEvents(file);
        var lines = new ArrayList<>(List.of("# Logbook"));
        lines.add("");
        lines.add("## All Events");
        lines.add("");
        lines.add("```text");
        for (var event : events) lines.add(event.toString().trim());
        lines.add("```");
        return List.copyOf(lines);
      } catch (Exception exception) {
        throw new RuntimeException("Failed to read recorded events?", exception);
      }
    }

    @Category({"Bach", "Logbook"})
    @Name("Bach.Logbook.LogEvent")
    @StackTrace(false)
    private static final class LogEvent extends Event {
      String level;
      String message;

      LogEvent(Level level, String message) {
        this.level = level.name();
        this.message = message;
      }
    }
  }
}
