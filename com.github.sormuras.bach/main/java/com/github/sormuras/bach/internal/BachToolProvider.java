package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Printer;
import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class BachToolProvider implements ToolProvider {
  @Override
  public String name() {
      return "bach";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    return Bach.of(Printer.of(out, err), args).run();
  }
}
