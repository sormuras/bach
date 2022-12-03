package run.bach.tool;

import java.io.PrintWriter;

import run.bach.Project;
import run.bach.ProjectTool;
import run.bach.Workbench;
import run.duke.ToolCall;

public class CleanTool extends ProjectTool {

  public static final String NAME = "clean";

  public CleanTool(Project project, Workbench workbench) {
    super(NAME, project, workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    // run(TreeTool.delete(folders().out()));
    run(ToolCall.of("tree").with("--mode", "delete").with(folders().out()));
    return 0;
  }
}
