package project;

import java.nio.file.Path;
import java.util.List;
import run.bach.Project;
import run.bach.ProjectTool;
import run.bach.ProjectToolRunner;

public class BachProjectFactory implements Project.Factory {
  public BachProjectFactory() {}

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

  @Override
  public Iterable<ProjectTool.Factory> createProjectToolFactories() {
    return List.of(Build::new, CompileClasses::new, Format::new);
  }
}
