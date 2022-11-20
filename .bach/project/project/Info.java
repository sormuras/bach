package project;

import run.bach.Bach;
import run.bach.Project;

public class Info implements Project.Factory {
  @Override
  public Project createProject(Bach bach) {
    return new Project(
        newProjectName(bach.cli(), "Bach"),
        newProjectVersion(bach.cli(), "2022.11-ea"),
        new Project.Space(
            "main",
            17,
            "run.bach/run.bach.Main",
            new Project.DeclaredModule(bach.paths().root("src/run.bach"))));
  }
}
