package run.bach.tool;

import java.io.PrintWriter;
import java.util.stream.Stream;

import run.bach.Project;
import run.bach.ProjectOperator;
import run.bach.Workbench;
import run.duke.ToolCall;

public class LaunchTool extends ProjectOperator {
  public static final String NAME = "launch";

  public LaunchTool(Project project, Workbench workbench) {
    super(NAME, project, workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var main = project().spaces().space("main");
    var launcher = main.launcher().orElseThrow(() -> new Error("No launcher defined. No start."));
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
