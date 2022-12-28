package run.bach.tool;

import java.io.PrintWriter;
import run.bach.Bach;
import run.duke.Duke;

public class CleanTool implements Bach.Operator {
  public CleanTool() {}

  @Override
  public final String name() {
    return "clean";
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    bach.run(Duke.treeDelete(bach.folders().out()));
    return 0;
  }
}
