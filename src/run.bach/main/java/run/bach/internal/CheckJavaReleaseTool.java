package run.bach.internal;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public record CheckJavaReleaseTool() implements ToolProvider {
  @Override
  public String name() {
    return "check-java-release";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (args.length != 1) return 1;
    var expected = Integer.parseInt(args[0]);
    var actual = Runtime.version().feature();
    if (actual == expected) return 0;
    err.println("Java release feature number check failed!");
    err.println("Expected exactly Java " + expected + ", but running on Java " + actual);
    return 1;
  }
}
