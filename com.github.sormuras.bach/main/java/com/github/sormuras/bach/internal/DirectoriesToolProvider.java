package com.github.sormuras.bach.internal;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.spi.ToolProvider;

public record DirectoriesToolProvider() implements ToolProvider {

  @Override
  public String name() {
    return "directories";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (args.length != 2) {
      out.println("""
          Usage: directories [create|clean|delete] PATH
          """);
      return 1;
    }
    try {
      var mode = Mode.valueOf(args[0].toUpperCase(Locale.ROOT));
      var path = Path.of(args[1]);
      if (mode == Mode.DELETE || mode == Mode.CLEAN) deleteDirectories(path);
      if (mode == Mode.CREATE || mode == Mode.CLEAN) createDirectories(path);
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 2;
    }
    return 0;
  }

  static void createDirectories(Path root) throws Exception {
    Files.createDirectories(root);
  }

  static void deleteDirectories(Path root) throws Exception {
    if (Files.notExists(root)) return;
    try (var stream = Files.walk(root)) {
      var paths = stream.sorted((p, q) -> -p.compareTo(q));
      for (var path : paths.toArray(Path[]::new)) Files.deleteIfExists(path);
    }
  }

  private enum Mode {
    CREATE,
    CLEAN,
    DELETE
  }
}
