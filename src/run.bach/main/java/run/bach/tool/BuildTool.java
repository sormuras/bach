package run.bach.tool;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import run.bach.Project;
import run.bach.ProjectTool;
import run.bach.ProjectToolRunner;

public class BuildTool extends ProjectTool {
  public static final String NAME = "build";

  public BuildTool(Project project, ProjectToolRunner runner) {
    super(NAME, project, runner);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var what = project().toNameAndVersion();
    var size = project().modules().size();

    info("Build %s with %d module%s".formatted(what, size, size == 1 ? "" : "s"));
    var start = Instant.now();

    run(CacheTool.NAME); // go offline and verify cached assets
    run(CompileTool.NAME); // compile all modules spaces
    run(TestTool.NAME); // start launcher and execute tests in test space

    var duration = prettify(Duration.between(start, Instant.now()));
    info("Build took %s".formatted(duration));

    return 0;
  }

  static String prettify(Duration duration) {
    var string = duration.truncatedTo(ChronoUnit.MILLIS).toString(); // ISO-8601: "PT8H6M12.345S"
    return string.substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase(Locale.ROOT);
  }
}
