package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;

public class Build implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    bach.run("cache"); // go offline

    for (var space : bach.project().spaces().list()) {
      if (space.modules().list().isEmpty()) {
        out.println("No modules declared in %s space.".formatted(space.name()));
        continue;
      }
      bach.run("compile", space.name()); // translate Java source files into class files
      bach.run("conserve", space.name()); // store compiled classes in modular JAR files
    }

    bach.run("test");
    return 0;
  }
}
