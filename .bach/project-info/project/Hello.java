package project;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class Hello implements ToolProvider {
  @Override
  public String name() {
    return "hello";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    out.println("hell");
    err.println("o");
    return 0;
  }
}
