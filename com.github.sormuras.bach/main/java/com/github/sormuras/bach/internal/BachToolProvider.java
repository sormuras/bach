package com.github.sormuras.bach.internal;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class BachToolProvider implements ToolProvider {
  @Override
  public String name() {
    return "bach";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    out.println("TODO BachToolProvider.run(...)");
    return 0;
  }
}
