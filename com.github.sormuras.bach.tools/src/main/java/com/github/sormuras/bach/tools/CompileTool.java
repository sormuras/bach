package com.github.sormuras.bach.tools;

import com.github.sormuras.bach.project.Project;
import java.io.PrintWriter;

record CompileTool(Project project) {
  void run(PrintWriter out, PrintWriter err) {
    out.printf("Compiling %s...%n", project);
  }
}
