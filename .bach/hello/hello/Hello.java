package hello;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class Hello implements ToolProvider {
  @Override
  public String name() {
    return "hello";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    out.println("hello " + (args.length == 0 ? "world" : String.join(" ", args)));
    return 0;
  }
}
