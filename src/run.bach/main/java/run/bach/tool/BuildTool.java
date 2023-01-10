package run.bach.tool;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import run.bach.ProjectOperator;
import run.bach.ProjectRunner;
import run.duke.ToolLogger;

public class BuildTool implements ProjectOperator {
  public BuildTool() {}

  @Override
  public final String name() {
    return "build";
  }

  @Override
  public void run(ProjectRunner runner, ToolLogger logger, String... args) {
    var what = runner.project().toNameAndVersion();
    var size = runner.project().modules().size();

    logger.log("Build %s with %d module%s".formatted(what, size, size == 1 ? "" : "s"));
    var start = Instant.now();

    runner.run(CacheTool.cache()); // go offline and verify cached assets
    runner.run(CompileTool.compile()); // compile all modules spaces
    runner.run(TestTool.test()); // start launcher and execute tests in test space

    var duration = prettify(Duration.between(start, Instant.now()));
    logger.log("Build took %s".formatted(duration));
  }

  static String prettify(Duration duration) {
    var string = duration.truncatedTo(ChronoUnit.MILLIS).toString(); // ISO-8601: "PT8H6M12.345S"
    return string.substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase(Locale.ROOT);
  }
}
