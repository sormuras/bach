package project;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class ProjectLocalTool implements ToolProvider {
  @Override
  public String name() {
    return "not";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    return 0;
  }
}
