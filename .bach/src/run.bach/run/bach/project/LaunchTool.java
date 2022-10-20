package run.bach.project;

import java.util.List;
import run.bach.Bach;
import run.bach.ToolCall;
import run.bach.ToolOperator;

public class LaunchTool implements ToolOperator {

  static final String NAME = "launch";

  public LaunchTool() {}

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    var main = bach.project().spaces().main();
    var launcher = main.launcher().orElseThrow(() -> new Error("No launcher defined. No start."));
    var java =
        ToolCall.of("java")
            .with("--module-path", bach.paths().out("main", "modules"))
            .with("--module", launcher)
            .with(arguments.stream());
    bach.run(java);
  }
}
