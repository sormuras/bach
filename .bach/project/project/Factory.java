package project;

import java.nio.file.Path;
import run.bach.Project;
import run.bach.ProjectFactory;
import run.bach.ProjectToolRunner;

public class Factory implements ProjectFactory {
  public Factory() {}

  @Override
  public Project createProject(ProjectToolRunner runner) {
    return new Project(
        new Project.Name("Bach"),
        new Project.Version("2022-ea"),
        new Project.Space(
            "main",
            17,
            "run.bach.Main",
            new Project.DeclaredModule(Path.of("src/run.bach/main/java")),
            new Project.DeclaredModule(Path.of("src/run.duke/main/java"))));
  }
}
