package run.bach.toolbox;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.spi.ToolProvider;
import run.bach.ToolCall;

public class TreeTool implements ToolProvider {

  public static ToolCall clean(Path root) {
    return new ToolCall("tree", List.of("--mode=clean", root.toString()));
  }

  @Override
  public String name() {
    return "tree";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var cli = new CLI().withParsingCommandLineArguments(List.of(args));
    var mode = cli.mode();
    var path = cli.paths().isEmpty() ? Path.of("") : cli.paths().get(0);
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
      if (mode == Mode.DELETE || mode == Mode.CLEAN && Files.exists(path)) {
        try (var stream = Files.walk(path)) {
          var files = stream.sorted((p, q) -> -p.compareTo(q));
          for (var file : files.toArray(Path[]::new)) Files.deleteIfExists(file);
        }
      }
      if (mode == Mode.CREATE || mode == Mode.CLEAN) {
        Files.createDirectories(path);
      }
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 2;
    }
    return 0;
  }

  public enum Mode {
    CREATE,
    CLEAN,
    DELETE,
    PRINT
  }

  record CLI(Optional<Boolean> __help, Optional<String> __mode, List<Path> paths) {
    CLI() {
      this(Optional.empty(), Optional.empty(), List.of());
    }

    boolean help() {
      return __help.orElse(false);
    }

    Mode mode() {
      return __mode.map(arg -> Mode.valueOf(arg.toUpperCase(Locale.ROOT))).orElse(Mode.PRINT);
    }

    CLI withParsingCommandLineArguments(List<String> args) {
      var arguments = new ArrayDeque<>(args);
      // extract
      var help = __help.orElse(null);
      var mode = __mode.orElse(null);
      var paths = new ArrayList<>(paths());
      // handle
      while (!arguments.isEmpty()) {
        var argument = arguments.removeFirst();
        /* parse flags */ {
          if (run.bach.CLI.HELP_FLAGS.contains(argument)) {
            help = Boolean.TRUE;
            continue;
          }
        }
        /* parse key-value pairs */ {
          int sep = argument.indexOf('=');
          var key = sep == -1 ? argument : argument.substring(0, sep);
          if (key.equals("--mode")) {
            mode = sep == -1 ? arguments.removeFirst() : argument.substring(sep + 1);
            continue;
          }
        }
        /* treat first unhandled argument as path */ {
          paths.add(Path.of(argument));
          break;
        }
      }
      arguments.stream().map(Path::of).forEach(paths::add);
      // compose
      return new CLI(Optional.ofNullable(help), Optional.ofNullable(mode), List.copyOf(paths));
    }
  }
}
