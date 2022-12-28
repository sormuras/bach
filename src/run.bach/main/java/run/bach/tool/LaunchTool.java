package run.bach.tool;

import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.util.stream.Stream;
import run.bach.Bach;
import run.duke.ToolCall;

public class LaunchTool implements Bach.Operator {

  public LaunchTool() {}

  @Override
  public final String name() {
    return "launch";
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var main = bach.project().spaces().space("main");
    var launchers = main.launchers();
    if (launchers.isEmpty()) {
      err.println("No launcher defined. No launch.");
      return 1;
    }
    if (launchers.size() > 1) {
      bach.printer().log(Level.DEBUG, "Using first launcher of: " + launchers);
    }
    var launcher = launchers.get(0);
    var modulePath = main.toRuntimeSpace().toModulePath(bach.folders()).orElse(".");
    var java =
        ToolCall.of("java")
            .with("--module-path", modulePath)
            .with("--module", launcher)
            .with(Stream.of(args));
    bach.run(java);
    return 0;
  }
}
