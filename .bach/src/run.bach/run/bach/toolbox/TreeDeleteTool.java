package run.bach.toolbox;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

public record TreeDeleteTool(String name) implements ToolProvider {
  public TreeDeleteTool() {
    this("tree-delete");
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (args.length != 1) {
      err.println("Exactly one argument expected, but got: " + args.length);
      return -1;
    }
    try (var stream = Files.walk(Path.of(args[0]))) {
      var files = stream.sorted((p, q) -> -p.compareTo(q));
      for (var file : files.toArray(Path[]::new)) Files.deleteIfExists(file);
      return 0;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }
}
