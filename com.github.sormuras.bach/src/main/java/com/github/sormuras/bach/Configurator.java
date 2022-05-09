package com.github.sormuras.bach;

import com.github.sormuras.bach.project.Project;

public interface Configurator {
  default Project configure(Project project) {
    return project;
  }
}
