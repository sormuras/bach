package com.github.sormuras.bach.tools;

import com.github.sormuras.bach.project.Project;

record TestTool(Commander commander, Project project) {
  void run() {
    commander.println("Testing %s...".formatted(project));
  }
}
