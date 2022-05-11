package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Tool;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Consumer;

public class Info implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    out.println("Configuration");
    out.println(bach.configuration().flags());
    out.println(bach.configuration().paths());
    out.println("Tools");
    var toolsBySimpleName = new TreeMap<String, List<Tool>>();
    for (var tool : bach.configuration().finder().findAll()) {
      var simpleName = tool.name().substring(tool.name().lastIndexOf('/') + 1);
      toolsBySimpleName.computeIfAbsent(simpleName, key -> new ArrayList<>()).add(tool);
    }
    for (var e : toolsBySimpleName.entrySet()) {
      var usedTool = e.getValue().get(0);
      out.printf("%16s -> %s [%s]%n", e.getKey(), usedTool.name(), getSimpleName(usedTool));
      Consumer<Tool> printer =
          tool ->
              out.print("%s%s [%s]%n".formatted(" ".repeat(20), tool.name(), getSimpleName(tool)));
      e.getValue().stream().skip(1).forEach(printer);
    }
    out.println("Project");
    out.println(bach.project().name());
    out.println(bach.project().version());
    return 0;
  }

  private static String getSimpleName(Tool tool) {
    return tool.provider().getClass().getSimpleName();
  }
}
