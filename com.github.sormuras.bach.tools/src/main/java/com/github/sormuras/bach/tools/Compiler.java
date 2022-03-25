package com.github.sormuras.bach.tools;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public record Compiler() implements ToolProvider {
  @Override
  public String name() {
    return "bach-tool-compile";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    return 0;
  }
}
