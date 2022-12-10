package test.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

record MockToolProvider(String name, int value) implements ToolProvider {
  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    return value;
  }
}
