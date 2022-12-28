package project;

import java.io.PrintWriter;
import run.duke.ToolOperator;
import run.duke.Workbench;

public final class Format implements ToolOperator {
  @Override
  public String name() {
    return "format";
  }

  @Override
  public int run(Workbench workbench, PrintWriter out, PrintWriter err, String... args) {
    if (workbench.find("google-java-format@1.15.0").isEmpty()) {
      workbench.run("install", "google-java-format@1.15.0");
    }
    workbench.run(
        "google-java-format@1.15.0", format -> format.with("--replace").withFindFiles("**.java"));
    return 0;
  }
}
