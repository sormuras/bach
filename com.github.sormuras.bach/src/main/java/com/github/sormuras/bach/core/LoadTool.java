package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.internal.StringSupport;
import java.io.PrintWriter;

public class LoadTool implements ToolOperator {
  @Override
  public String name() {
    return "load-tool";
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var name = args[0];
    var from = args[1];

    var directory = bach.configuration().paths().externalTools(name);
    var target = directory.resolve(StringSupport.parseFileName(from));

    bach.run("load-and-verify", target, from);
    return 0;
  }
}
