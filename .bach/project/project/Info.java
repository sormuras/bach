package project;

import java.time.ZonedDateTime;
import run.bach.Bach;
import run.bach.DeclaredModule;
import run.bach.Project;

public class Info implements Project.Factory {
  @Override
  public Project createProject(Bach bach) {
    var cli = bach.cli();
    return new Project(
        new Project.Name(cli.__project_name().orElse("Bach")),
        new Project.Version(
            cli.__project_version().orElse("2022.11-ea"),
            cli.__project_version_timestamp()
                .map(ZonedDateTime::parse)
                .orElseGet(ZonedDateTime::now)),
        new Project.Spaces(
            new Project.Space("main")
                .withTargetsJava(17)
                .withModules(DeclaredModule.of("src/run.bach", "src/run.bach/module-info.java"))
                .withLauncher("run.bach/run.bach.Main")),
        new Project.Externals().withRequires("java.base"));
  }
}
