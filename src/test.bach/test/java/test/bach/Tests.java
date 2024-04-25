package test.bach;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import run.bach.ToolSpace;

public record Tests(String name) implements ToolProvider {
  public static void main(String... args) {
    System.exit(new Tests().run(System.out, System.err, args));
  }

  public Tests() {
    this(Tests.class.getName());
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var silent = new ToolSpace(ToolSpace.Flag.SILENT);
    var run = silent.run("jar", "--version");
    var expected = "jar " + System.getProperty("java.version");
    if (!run.out().equals(expected)) {
      err.println("expected: " + expected);
      err.println("actual  : " + run.out());
      return 1;
    }
    return 0;
  }
}