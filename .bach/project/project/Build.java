package project;

import java.io.PrintWriter;
import run.bach.Bach;
import run.bach.tool.BuildTool;

public final class Build extends BuildTool {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    out.println("BEGIN");
    try {
      return super.run(bach, out, err, args);
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    } finally {
      out.println("END.");
    }
  }
}
