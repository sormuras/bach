package run.duke.base;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public record CheckJavaVersionTool() implements ToolProvider {
  @Override
  public String name() {
    return "check-java-version";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (args.length != 1) return 1;
    var actual = Runtime.version();
    var minimal = Runtime.Version.parse(args[0]);
    if (actual.compareTo(minimal) >= 0) return 0;
    err.println("Java version check failed!");
    err.println("At least Java " + minimal + " is required, but running on Java " + actual);
    return 1;
  }
}
