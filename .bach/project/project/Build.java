package project;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import run.bach.tool.BuildTool;
import run.duke.Workbench;

public final class Build extends BuildTool {
  public Build() {}

  private Build(Workbench workbench) {
    super(workbench);
  }

  @Override
  public ToolProvider provider(Workbench workbench) {
    return new Build(workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      // run("format");
      return super.run(out, err, args); // same run("run.bach/build");
    } catch (Exception exception) {
      printer().log(System.Logger.Level.ERROR, exception.toString());
      return 1;
    }
  }
}
