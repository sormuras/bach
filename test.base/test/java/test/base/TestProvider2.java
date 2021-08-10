package test.base;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

@Name("test")
public record TestProvider2() implements ToolProvider {

  @Override
  public String name() {
    return new Name.Support(TestProvider2.class).name();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    return 0;
  }
}
