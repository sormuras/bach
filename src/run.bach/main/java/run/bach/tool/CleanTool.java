package run.bach.tool;

import java.io.PrintWriter;
import run.bach.Project;
import run.bach.ProjectTool;
import run.bach.ProjectToolRunner;
import run.duke.DukeTool;

public class CleanTool extends ProjectTool {
  public static final String NAME = "clean";

  public CleanTool(Project project, ProjectToolRunner runner) {
    super(NAME, project, runner);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    run(DukeTool.treeDelete(folders().out()));
    return 0;
  }
}
