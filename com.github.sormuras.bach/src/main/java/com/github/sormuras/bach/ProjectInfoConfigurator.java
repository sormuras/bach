package com.github.sormuras.bach;

import com.github.sormuras.bach.project.Project;

public non-sealed interface ProjectInfoConfigurator extends ProjectWorkflowListener {

  static ProjectInfoConfigurator ofDefaults() {
    record DefaultConfigurator() implements ProjectInfoConfigurator {}
    return new DefaultConfigurator();
  }

  default Project configure(Project project) {
    return project;
  }
}
