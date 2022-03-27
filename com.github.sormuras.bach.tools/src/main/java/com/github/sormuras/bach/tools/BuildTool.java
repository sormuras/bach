package com.github.sormuras.bach.tools;

import com.github.sormuras.bach.project.Project;

record BuildTool(Commander commander, Project project) {
  void run() {
    commander.execute("banner", "BUILD");
    commander.println("Building %s...".formatted(project));

    new CompileTool(commander, project).run();
    new TestTool(commander, project).run();

    commander.execute("com.github.sormuras.bach.tools/test", 1, 23);
  }
}
