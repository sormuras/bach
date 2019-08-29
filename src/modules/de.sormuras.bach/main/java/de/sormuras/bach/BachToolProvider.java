package de.sormuras.bach;

import java.io.PrintWriter;
import java.util.List;
import java.util.spi.ToolProvider;

/** Provide Bach.java as a service. */
public class BachToolProvider implements ToolProvider {

  @Override
  public String name() {
    return "bach";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var bach = new Bach(out, err);
    try {
      bach.main(List.of(args));
      return 0;
    } catch (Throwable t) {
      return 1;
    }
  }
}
