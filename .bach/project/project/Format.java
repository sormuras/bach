package project;

import java.io.PrintWriter;
import run.duke.Tooling;
import run.duke.Workbench;

public record Format(Workbench workbench) implements Tooling {
  public Format() {
    this(Workbench.inoperative());
  }

  @Override
  public String name() {
    return "format";
  }

  @Override
  public Format provider(Workbench workbench) {
    return new Format(workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (find("google-java-format@1.15.0").isEmpty()) run("install", "google-java-format@1.15.0");
    run("google-java-format@1.15.0", format -> format.with("--replace").withFindFiles("**.java"));
    return 0;
  }
}
