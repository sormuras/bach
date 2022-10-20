package run.bach.toolbox;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

public record TreeTool(String name) implements ToolProvider {
  public TreeTool() {
    this("tree");
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var start = Path.of(args.length == 0 ? "." : args[0]);
    try (var stream = Files.walk(start, 99)) {
      stream
          .filter(Files::isDirectory)
          .map(Path::normalize)
          .map(Path::toString)
          .map(name -> name.replace('\\', '/'))
          .filter(name -> !name.contains(".git/"))
          .sorted()
          .map(name -> name.replaceAll(".+?/", "  "))
          .forEach(out::println);
      return 0;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }
}
