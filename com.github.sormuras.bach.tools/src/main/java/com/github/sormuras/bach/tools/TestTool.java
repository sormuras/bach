package com.github.sormuras.bach.tools;

import com.github.sormuras.bach.project.Project;
import java.io.PrintWriter;

record TestTool(Project project) {
  void run(PrintWriter out, PrintWriter err) {
    out.printf("Testing %s...%n", project);
  }
}
