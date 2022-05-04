package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Tool;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;
import java.util.Comparator;

public class Info implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    out.println("Configuration");
    out.println(bach.configuration().flags());
    out.println(bach.configuration().paths());
    out.println("Tools");
    bach.configuration().finder().findAll().stream()
        .sorted(Comparator.comparing(Tool::name))
        .forEach(tool -> out.printf("  - %s%n", tool.name()));
    out.println("Project");
    out.println(bach.project().name());
    out.println(bach.project().version());
    return 0;
  }
}
