package project;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class World implements ToolProvider {
  @Override
  public String name() {
    return "world";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    out.println(args.length == 0 ? "world" : String.join(" ", args));
    return 0;
  }
}
