package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;

public class Compile implements ToolOperator {

  static final String NAME = "compile";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    for (var space : bach.project().spaces().list()) {
      var modules = space.modules().list();
      if (modules.isEmpty()) {
        if (bach.configuration().isVerbose()) {
          out.println("No modules declared in %s space.".formatted(space.name()));
        }
        continue;
      }
      var s = modules.size() == 1 ? "" : "s";
      out.println("Compile %d module%s in %s space...".formatted(modules.size(), s, space.name()));
      bach.run(CompileClasses.NAME, space.name()); // translate Java source files into class files
      bach.run(CompileModules.NAME, space.name()); // archive compiled classes in modular JAR files
    }
    return 0;
  }
}
