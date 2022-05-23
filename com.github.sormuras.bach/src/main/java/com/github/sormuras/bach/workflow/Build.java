package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;

public class Build implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    bach.run(Cache.NAME); // go offline and verify cached assets
    bach.run(Compile.NAME); // compile all modules spaces
    bach.run(Test.NAME); // start launcher and execute testables in test space
    return 0;
  }
}
