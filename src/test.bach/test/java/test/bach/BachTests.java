package test.bach;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public record BachTests(String name) implements ToolProvider {
  public BachTests() {
    this(BachTests.class.getName());
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    return 0;
  }
}
