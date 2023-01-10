package run.bach.tool;

import java.util.stream.Stream;
import run.bach.ProjectOperator;
import run.bach.ProjectRunner;
import run.duke.ToolCall;
import run.duke.ToolLogger;

public class LaunchTool implements ProjectOperator {

  public LaunchTool() {}

  @Override
  public final String name() {
    return "launch";
  }

  @Override
  public void run(ProjectRunner runner, ToolLogger logger, String... args) {
    var main = runner.project().spaces().space("main");
    var launchers = main.launchers();
    if (launchers.isEmpty()) {
      logger.error("No launcher defined. No launch.");
      return;
    }
    if (launchers.size() > 1) {
      logger.debug("Using first launcher of: " + launchers);
    }
    var launcher = launchers.get(0);
    var modulePath = main.toRuntimeSpace().toModulePath(runner.folders()).orElse(".");
    var java =
        ToolCall.of("java")
            .with("--module-path", modulePath)
            .with("--module", launcher)
            .with(Stream.of(args));
    runner.run(java);
  }
}
