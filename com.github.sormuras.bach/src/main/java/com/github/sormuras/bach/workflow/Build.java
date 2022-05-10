package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;

public class Build implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    bach.run("cache"); // go offline
    bach.run("compile");
    bach.run("test");
    return 0;
  }
}