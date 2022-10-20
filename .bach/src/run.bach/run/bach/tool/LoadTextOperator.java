package run.bach.tool;

import java.net.URI;
import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;

public record LoadTextOperator(String name) implements ToolOperator {
  public LoadTextOperator() {
    this("load-text");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    // if (help(bach, arguments, "<uris...>")) return;
    bach.info(bach.browser().read(URI.create(arguments.get(0))));
  }
}
