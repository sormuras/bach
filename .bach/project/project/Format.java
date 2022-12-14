package project;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import run.duke.ToolOperator;
import run.duke.ToolRunner;
import run.duke.ToolTrait;

public record Format(ToolRunner toolRunner) implements ToolOperator, ToolProvider, ToolTrait {
  public Format() {
    this(ToolRunner.nullRunner());
  }

  @Override
  public String name() {
    return "format";
  }

  @Override
  public ToolProvider provider(ToolRunner runner) {
    return new Format(runner);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (find("google-java-format@1.15.0").isEmpty()) run("install", "google-java-format@1.15.0");
    run("google-java-format@1.15.0", format -> format.with("--replace").withFindFiles("**.java"));
    return 0;
  }
}
