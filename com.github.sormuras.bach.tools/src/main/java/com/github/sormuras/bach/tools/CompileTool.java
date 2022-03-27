package com.github.sormuras.bach.tools;

import com.github.sormuras.bach.project.Project;

record CompileTool(Commander commander, Project project) {
  void run() {
    commander.println("Compiling %s...".formatted(project));
  }
}
