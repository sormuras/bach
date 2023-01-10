package run.duke.tool;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.spi.ToolProvider;
import run.duke.CommandLineInterface.Help;
import run.duke.CommandLineInterface.Name;
import run.duke.Duke;

public record CheckJavaVersionTool() implements ToolProvider {
  record Options(
      @Name({"--help", "-help", "-h", "/?", "?"}) @Help("Print help message") F1 help,
      @Help("The minimally expected Java runtime version") Runtime.Version version) {
    record F1() {}
  }

  @Override
  public String name() {
    return "check-java-version";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var options = Duke.splitter(MethodHandles.lookup(), Options.class).split(args);
    if (options.help() != null) {
      out.println("Usage: %s expected".formatted(name()));
      return 0;
    }
    var expected = options.version();
    var actual = Runtime.version();
    if (actual.compareTo(expected) >= 0) return 0;
    err.println("Java version check failed!");
    err.println("At least Java " + expected + " is required, but running on Java " + actual);
    return 1;
  }
}
