package org.example.lib.internal;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public record EchoToolProvider(String name) implements ToolProvider {
  public EchoToolProvider() {
    this("echo");
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    out.printf("echo " + String.join(" ", args));
    return 0;
  }
}
