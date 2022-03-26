package com.github.sormuras.bach.tools;

import com.github.sormuras.bach.project.Project;
import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public record CompileToolProvider() implements ToolProvider {
  @Override
  public String name() {
    return "compile";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      new CompileTool(new Project()).run(out, err);
      return 0;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }
}
