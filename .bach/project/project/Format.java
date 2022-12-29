package project;

import java.io.PrintWriter;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public final class Format implements ToolOperator {
  @Override
  public String name() {
    return "format";
  }

  @Override
  public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
    var formatter = "google-java-format@1.15.0";
    if (runner.findTool(formatter).isEmpty()) runner.run("install", formatter);
    runner.run(formatter, call -> call.with("--replace").withFindFiles("**.java"));
    return 0;
  }
}
