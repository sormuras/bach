package com.github.sormuras.bach.core;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

public class Tree implements ToolProvider {
  enum Mode {
    CREATE,
    CLEAN,
    DELETE,
    PRINT
  }

  @Override
  public String name() {
    return "tree";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
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
}
