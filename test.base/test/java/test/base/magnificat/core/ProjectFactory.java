package test.base.magnificat.core;

import java.lang.module.ModuleDescriptor.Version;
import test.base.magnificat.Configuration;
import test.base.magnificat.api.Option;
import test.base.magnificat.api.Project;

public class ProjectFactory {

  private final Configuration main;

  public ProjectFactory(Configuration main) {
    this.main = main;
  }

  public Project newProject() {
    var name = main.options().get(Option.PROJECT_NAME);
    var version = main.options().get(Option.PROJECT_VERSION);
    return new Project(name, Version.parse(version));
  }
}
