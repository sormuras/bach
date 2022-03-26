package com.github.sormuras.bach.tools;

import com.github.sormuras.bach.project.Project;
import java.io.PrintWriter;

record BuildTool(Project project) {
  void run(PrintWriter out, PrintWriter err) {
    out.printf("Building %s...%n", project);
    new CompileTool(project).run(out, err);
    new TestTool(project).run(out, err);
  }
}
