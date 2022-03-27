package com.github.sormuras.bach.tools;

import com.github.sormuras.bach.project.Project;
import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public record TestToolProvider() implements ToolProvider {
  @Override
  public String name() {
    return "test";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      var commander = Commander.of(out, err);
      var project = new Project();
      new TestTool(commander, project).run();
      return 0;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }
}
