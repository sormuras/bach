package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Tool;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;
import java.util.Comparator;

public class Help implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    out.print("""
        Usage: Bach [OPTIONS] TOOL-NAME [TOOL-ARGS...]
        """);
    if (bach.configuration().isVerbose()) {
      out.println("Available tools include:");
      bach.configuration().finder().findAll().stream()
          .filter(Tool::isNotHidden)
          .sorted(Comparator.comparing(Tool::name))
          .forEach(tool -> out.printf("  - %s%n", tool.name()));
    }
    return 0;
  }
}
