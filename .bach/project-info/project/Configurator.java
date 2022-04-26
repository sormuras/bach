package project;

import com.github.sormuras.bach.project.Project;

public class Configurator implements Project.Configurator {
  @Override
  public Project configure(Project project) {
    return project.withParsingArguments(
        """
        --project-version
          17-M5
        """.lines().toList());
  }
}
