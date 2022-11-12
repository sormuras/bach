package run.bach.project;

import java.time.Duration;
import java.time.Instant;
import run.bach.ToolOperator;
import run.bach.internal.StringSupport;

public class BuildTool implements ToolOperator {
  public BuildTool() {}

  @Override
  public String name() {
    return "build";
  }

  @Override
  public void run(Operation operation) {
    var start = Instant.now();
    var bach = operation.bach();
    var what = bach.project().toNameAndVersion();
    var size = bach.project().modules().size();
    bach.info("Build %s with %d module%s".formatted(what, size, size == 1 ? "" : "s"));

    operation.run(CacheTool.class); // go offline and verify cached assets
    operation.run(CompileTool.class); // compile all modules spaces
    operation.run(TestTool.class); // start launcher and execute tests in test space

    var duration = StringSupport.prettify(Duration.between(start, Instant.now()));
    bach.info("Build took %s".formatted(duration));
  }
}
