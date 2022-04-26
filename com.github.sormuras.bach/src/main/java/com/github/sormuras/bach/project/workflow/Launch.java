package com.github.sormuras.bach.project.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;

public class Launch implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    /*
    var launcher = bach.configuration.project.spaces().main().launcher();
    var paths = bach.configuration.paths;
    if (launcher.isEmpty()) {
      err.println("No launcher defined. No start.");
      return 1;
    }

    var java =
        Tool.Call.of("java")
            .with("--module-path", paths.out("main", "modules"))
            .with("--module", launcher.get())
            .with(Stream.of(args));
    bach.run(java);
    */
    return 0;
  }
}
