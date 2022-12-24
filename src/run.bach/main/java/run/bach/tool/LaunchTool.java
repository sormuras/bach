package run.bach.tool;

import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.util.stream.Stream;
import run.bach.ProjectTool;
import run.duke.ToolCall;
import run.duke.Workbench;

public class LaunchTool extends ProjectTool {

  public LaunchTool() {}

  protected LaunchTool(Workbench workbench) {
    super(workbench);
  }

  @Override
  public final String name() {
    return "launch";
  }

  @Override
  public LaunchTool provider(Workbench workbench) {
    return new LaunchTool(workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var main = project().spaces().space("main");
    var launchers = main.launchers();
    if (launchers.isEmpty()) {
      err.println("No launcher defined. No launch.");
      return 1;
    }
    if (launchers.size() > 1) {
      printer().log(Level.DEBUG, "Using first launcher of: " + launchers);
    }
    var launcher = launchers.get(0);
    var modulePath = main.toRuntimeSpace().toModulePath(folders()).orElse(".");
    var java =
        ToolCall.of("java")
            .with("--module-path", modulePath)
            .with("--module", launcher)
            .with(Stream.of(args));
    run(java);
    return 0;
  }
}
