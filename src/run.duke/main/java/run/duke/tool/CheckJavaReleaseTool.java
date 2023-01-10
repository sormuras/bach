package run.duke.tool;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.spi.ToolProvider;
import run.duke.CommandLineInterface.Help;
import run.duke.CommandLineInterface.Name;
import run.duke.Duke;

public record CheckJavaReleaseTool() implements ToolProvider {
  record Options(
      @Name({"--help", "-help", "-h", "/?", "?"}) @Help("Print help message") F1 help,
      @Help("The expected Java release feature number") Integer release) {
    record F1() {}
  }

  @Override
  public String name() {
    return "check-java-release";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var options = Duke.splitter(MethodHandles.lookup(), Options.class).split(args);
    if (options.help() != null) {
      out.println("Usage: %s expected".formatted(name()));
      return 0;
    }
    var expected = options.release();
    var actual = Runtime.version().feature();
    if (actual == expected) return 0;
    err.println("Java release feature number check failed!");
    err.println("Expected exactly Java " + expected + ", but running on Java " + actual);
    return 1;
  }
}
