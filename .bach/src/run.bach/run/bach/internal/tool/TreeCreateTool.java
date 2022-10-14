package run.bach.internal.tool;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

public record TreeCreateTool(String name) implements ToolProvider {
  public TreeCreateTool() {
    this("tree-create");
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (args.length != 1) {
      err.println("Exactly one argument expected, but got: " + args.length);
      return -1;
    }
    try {
      Files.createDirectories(Path.of(args[0]));
      return 0;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }
}
