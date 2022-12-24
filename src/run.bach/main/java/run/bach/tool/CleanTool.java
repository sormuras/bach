package run.bach.tool;

import java.io.PrintWriter;
import run.bach.ProjectTool;
import run.duke.Duke;
import run.duke.Workbench;

public class CleanTool extends ProjectTool {
  public CleanTool() {}

  protected CleanTool(Workbench workbench) {
    super(workbench);
  }

  @Override
  public final String name() {
    return "clean";
  }

  @Override
  public CleanTool provider(Workbench workbench) {
    return new CleanTool(workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    run(Duke.treeDelete(folders().out()));
    return 0;
  }
}
