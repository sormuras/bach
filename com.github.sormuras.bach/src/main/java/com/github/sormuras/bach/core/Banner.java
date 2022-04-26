package com.github.sormuras.bach.core;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class Banner implements ToolProvider {
  @Override
  public String name() {
    return "banner";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (args.length == 0) {
      err.println("Usage: banner TEXT");
      return 1;
    }
    var text = String.join(" ", args);
    var line = "=".repeat(text.length());
    out.println("""
            %s
            %s
            %s""".formatted(line, text, line));
    return 0;
  }
}
