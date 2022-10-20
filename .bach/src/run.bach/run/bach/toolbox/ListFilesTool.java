package run.bach.toolbox;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

public record ListFilesTool(String name) implements ToolProvider {
  public ListFilesTool() {
    this("list-files");
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var start = Path.of("");
    var pattern = args.length == 0 ? "*" : args[0];
    var syntaxAndPattern = pattern.contains(":") ? pattern : "glob:" + pattern;
    var matcher = start.getFileSystem().getPathMatcher(syntaxAndPattern);
    try (var stream = Files.find(start, 99, (path, attr) -> matcher.matches(path))) {
      stream.filter(path -> !start.equals(path)).forEach(out::println);
      return 0;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }
}
