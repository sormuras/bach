package test.modules;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class TestProvider implements ToolProvider {

  @Override
  public String name() {
    return "test(test.modules)";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    out.println("out");
    err.println("err");
    return 0;
  }
}
