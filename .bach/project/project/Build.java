package project;

import java.io.PrintWriter;
import run.bach.Bach;
import run.bach.tool.BuildTool;

public final class Build extends BuildTool {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    try {
      super.run(bach, out, err, args); // same as run("run.bach/build");
      bach.run("zip", ".bach/out/bach.zip");
      bach.run("jar", "--list", "--file", ".bach/out/bach.zip");
      return 0;
    } catch (Exception exception) {
      bach.printer().log(System.Logger.Level.ERROR, exception.toString());
      return 1;
    }
  }
}
