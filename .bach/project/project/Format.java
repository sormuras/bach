package project;

import java.io.PrintWriter;
import run.bach.Project;
import run.bach.ProjectTool;
import run.bach.ProjectToolRunner;

public final class Format extends ProjectTool {
  public Format(Project project, ProjectToolRunner runner) {
    super("format", project, runner);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (find("google-java-format@1.15.0").isEmpty()) run("install", "google-java-format@1.15.0");
    run("google-java-format@1.15.0", format -> format.with("--replace").withFindFiles("**.java"));
    return 0;
  }
}
