package com.github.sormuras.bach.tools;

import com.github.sormuras.bach.project.Project;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public record BuildToolProvider() implements ToolProvider {
  @Override
  public String name() {
    return "build";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    out.printf("Building %s...%n", new Project());
    return 0;
  }
}
