package project;

import run.bach.Bach;
import run.bach.Project;

public class Info implements Project.Factory {
  @Override
  public Project createProject(Bach bach) {
    var cli = bach.cli();
    var paths = bach.paths();
    return new Project(
        new Project.Name(cli.projectName("Bach")),
        new Project.Version(cli.projectVersion("2022.11-ea"))
            .with(cli.projectVersionTimestampOrNow()),
        new Project.Spaces(
            new Project.Space("main")
                .withTargetsJava(17)
                .withModule(paths.root("src/run.bach"), paths.root("src/run.bach/module-info.java"))
                .withLauncher("run.bach/run.bach.Main")),
        new Project.Externals().withRequires("java.base"));
  }
}
