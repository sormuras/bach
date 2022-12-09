package project;

import java.io.PrintWriter;
import run.bach.Project;
import run.bach.ProjectToolRunner;
import run.bach.tool.BuildTool;

public final class Build extends BuildTool {
  public Build(Project project, ProjectToolRunner runner) {
    super(project, runner);
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
