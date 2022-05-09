package com.github.sormuras.bach.project.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;

public class Build implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    bach.configuration().projectWorkflowListener().onWorklowBuildBegin(bach);

    bach.run("cache"); // go offline
    bach.run("compile");

    bach.configuration().projectWorkflowListener().onWorklowBuildEnd(bach);
    return 0;
  }
}
