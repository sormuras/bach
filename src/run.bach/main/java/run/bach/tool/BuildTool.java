package run.bach.tool;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import run.bach.Bach;

public class BuildTool implements Bach.Operator {
  public BuildTool() {}

  @Override
  public final String name() {
    return "build";
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var what = bach.project().toNameAndVersion();
    var size = bach.project().modules().size();

    out.println("Build %s with %d module%s".formatted(what, size, size == 1 ? "" : "s"));
    var start = Instant.now();

    bach.run(CacheTool.cache()); // go offline and verify cached assets
    bach.run(CompileTool.compile()); // compile all modules spaces
    bach.run(TestTool.test()); // start launcher and execute tests in test space

    var duration = prettify(Duration.between(start, Instant.now()));
    out.println("Build took %s".formatted(duration));

    return 0;
  }

  static String prettify(Duration duration) {
    var string = duration.truncatedTo(ChronoUnit.MILLIS).toString(); // ISO-8601: "PT8H6M12.345S"
    return string.substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase(Locale.ROOT);
  }
}
