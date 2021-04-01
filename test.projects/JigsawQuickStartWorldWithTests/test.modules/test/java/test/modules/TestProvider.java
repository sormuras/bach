package test.modules;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class TestProvider implements ToolProvider {
  @Override
  public String name() {
    return "test";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    return 0;
  }
}
