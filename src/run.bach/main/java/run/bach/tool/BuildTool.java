package run.bach.tool;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.spi.ToolProvider;
import run.bach.ProjectTool;
import run.duke.Workbench;

public class BuildTool extends ProjectTool {
  public BuildTool() {
    super();
  }

  protected BuildTool(Workbench workbench) {
    super(workbench);
  }

  @Override
  public final String name() {
    return "build";
  }

  @Override
  public ToolProvider provider(Workbench workbench) {
    return new BuildTool(workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var what = project().toNameAndVersion();
    var size = project().modules().size();

    info("Build %s with %d module%s".formatted(what, size, size == 1 ? "" : "s"));
    var start = Instant.now();

    run(CacheTool.cache()); // go offline and verify cached assets
    run(CompileTool.compile()); // compile all modules spaces
    run(TestTool.test()); // start launcher and execute tests in test space

    var duration = prettify(Duration.between(start, Instant.now()));
    info("Build took %s".formatted(duration));

    return 0;
  }

  static String prettify(Duration duration) {
    var string = duration.truncatedTo(ChronoUnit.MILLIS).toString(); // ISO-8601: "PT8H6M12.345S"
    return string.substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase(Locale.ROOT);
  }
}
