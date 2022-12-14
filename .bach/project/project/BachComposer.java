package project;

import java.nio.file.Path;
import java.util.List;
import run.bach.Composer;
import run.bach.Project;

public class BachComposer extends Composer {
  @Override
  public Project composeProject() {
    return new Project(
        new Project.Name("Bach"),
        new Project.Version("2022-ea"),
        new Project.Space(
            "main",
            17,
            "run.bach/run.bach.Main",
            new Project.DeclaredModule(Path.of("src/run.bach/main/java")),
            new Project.DeclaredModule(Path.of("src/run.duke/main/java"))),
        new Project.Space(
            "test",
            List.of("main"),
            0,
            List.of("test.duke/test.duke.Main", "test.bach/test.bach.Main"),
            new Project.DeclaredModules(
                new Project.DeclaredModule(Path.of("src/test.bach/test/java")),
                new Project.DeclaredModule(Path.of("src/test.duke/test/java")))));
  }

  @Override
  public List<Operator> composeOperators() {
    return List.of(
        Operator.of("build", Build::new),
        Operator.of("compile-classes", CompileClasses::new),
        Operator.of("format", Format::new));
  }
}
