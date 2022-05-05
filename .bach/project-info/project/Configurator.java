package project;

import com.github.sormuras.bach.Main;
import com.github.sormuras.bach.project.Project;

public class Configurator implements Project.Configurator {
  @Override
  public Project apply(Project project) {
    return project
        .withTargetsJava(17)
        .withTargetsJava(project.spaces().test(), Runtime.version().feature())
        .withLauncher(Main.class.getCanonicalName());
  }
}
