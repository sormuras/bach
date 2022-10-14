package run.bach.internal.tool;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;

public record LoadFileOperator(String name) implements ToolOperator {
  public LoadFileOperator() {
    this("load-file");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    // if (help(bach, arguments, "<from-uri> [<to-path>]")) return;
    var uri = URI.create(arguments.get(0));
    var path = Path.of(arguments.get(1));
    bach.browser().load(uri, path);
  }
}
