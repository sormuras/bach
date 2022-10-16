package run.bach.project.workflow;

import java.util.List;
import run.bach.Bach;
import run.bach.ToolCall;
import run.bach.ToolOperator;

public class Cache implements ToolOperator {

  static final String NAME = "cache";

  public Cache() {}

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    if (!arguments.isEmpty()) {
      bach.run(ToolCall.of("load-modules").with(arguments.stream()));
      return;
    }
    bach.run(ToolCall.of("load-modules").with(bach.project().externals().requires().stream()));
  }
}
