package test.bach;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import run.bach.ToolRunner;

public record Tests(String name) implements ToolProvider {
  public static void main(String... args) {
    System.exit(new Tests().run(System.out, System.err, args));
  }

  public Tests() {
    this(Tests.class.getName());
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var actual = ToolRunner.ofSilence().run("jar", "--version").out();
    var expected = "jar " + System.getProperty("java.version");
    if (actual.equals(expected)) return 0;
    err.println("expected: " + expected);
    err.println("actual  : " + actual);
    return 1;
  }
}
