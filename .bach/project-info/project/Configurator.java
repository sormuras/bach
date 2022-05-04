package project;

import com.github.sormuras.bach.project.Project;

public class Configurator implements Project.Configurator {
  @Override
  public Project apply(Project project) {
    return project.withVersion("17-M5");
  }
}
