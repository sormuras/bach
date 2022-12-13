package run.duke.tool;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.spi.ToolProvider;
import run.duke.CommandLineInterface;

public record TreeTool() implements ToolProvider {
  record Options(Optional<String> __mode, String... paths) {
    enum Mode {
      CREATE,
      CLEAN,
      DELETE,
      PRINT
    }

    Mode mode() {
      return __mode.map(arg -> Mode.valueOf(arg.toUpperCase(Locale.ROOT))).orElse(Mode.PRINT);
    }
  }

  @Override
  public String name() {
    return "tree";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var cli = CommandLineInterface.of(MethodHandles.lookup(), Options.class).split(args);
    var mode = cli.mode();

    try {
      if (mode == Options.Mode.PRINT) {
        var path = cli.paths().length == 0 ? Path.of("") : Path.of(cli.paths()[0]);
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
        return 0;
      }
      if (cli.paths.length != 1)
        throw new IllegalArgumentException("Expected exactly one file argument: " + cli);
      var start = Path.of(cli.paths[0]);
      // TODO start must not be a root directory
      // TODO start must not be the current working directory
      if ((mode == Options.Mode.DELETE || mode == Options.Mode.CLEAN) && Files.exists(start)) {
        try (var stream = Files.walk(start)) {
          var files = stream.sorted((p, q) -> -p.compareTo(q));
          for (var file : files.toArray(Path[]::new)) Files.deleteIfExists(file);
        }
      }
      if ((mode == Options.Mode.CREATE || mode == Options.Mode.CLEAN) && Files.notExists(start)) {
        Files.createDirectories(start);
      }
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 2;
    }
    return 0;
  }
}