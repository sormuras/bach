package project;

import java.nio.file.Path;
import java.util.List;
import run.bach.Options;
import run.bach.Project;
import run.bach.Workbench;

public class BachWorkbench implements Workbench {
  public BachWorkbench() {}

  @Override
  public Project createProject(Options options) {
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
  public List<Operator> createProjectTools() {
    return List.of(
        Operator.of("build", Build::new),
        Operator.of("compile-classes", CompileClasses::new),
        Operator.of("format", Format::new));
  }
}
