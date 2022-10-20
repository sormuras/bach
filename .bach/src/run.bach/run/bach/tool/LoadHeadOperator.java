package run.bach.tool;

import java.net.URI;
import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;

public record LoadHeadOperator(String name) implements ToolOperator {
  public LoadHeadOperator() {
    this("load-head");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    // if (help(bach, arguments, "<uris...>")) return;
    for (var argument : arguments) {
      var uri = URI.create(argument);
      var head = bach.browser().head(uri);
      bach.info(head);
      for (var entry : head.headers().map().entrySet()) {
        bach.debug(entry.getKey());
        for (var line : entry.getValue()) bach.debug("  " + line);
      }
    }
  }
}
