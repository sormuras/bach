package run.bach.project;

import java.util.List;
import run.bach.Bach;
import run.bach.ToolCall;
import run.bach.ToolOperator;

public class CacheTool implements ToolOperator {

  static final String NAME = "cache";

  public CacheTool() {}

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    if (!arguments.isEmpty()) {
      bach.run(ToolCall.of("load", "modules").with(arguments.stream()));
      return;
    }
    bach.run(ToolCall.of("load", "modules").with(bach.project().externals().requires().stream()));
  }
}
