package project;

import java.nio.file.Path;
import java.util.List;
import run.bach.Composer;
import run.bach.Project;
import run.bach.ProjectInfo;
import run.bach.ProjectTools;
import run.bach.Tweaks;

public class BachComposer extends Composer {
  @Override
  public Project createProject() {
    var info = ProjectInfo.Support.of(getClass().getModule());
    return new Project(
        new Project.Name(options.projectName(info.name())),
        new Project.Version(options.projectVersionOrNow(), options.projectVersionTimestampOrNow()),
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
                new Project.DeclaredModule(Path.of("src/test.duke/test/java")),
                new Project.DeclaredModule(Path.of("src/test.junit/test/java")))));
  }

  @Override
  public ProjectTools createProjectTools() {
    return new ProjectTools(new Build(), new CompileClasses());
  }

  @Override
  public Tweaks createTweaks() {
    return new Tweaks(
        call ->
            switch (call.tool()) {
              case "javac" -> call.withTweak(0, tweak -> tweak.with("-g").with("-parameters"));
              case "junit" -> call.with("--details", "NONE").with("--disable-banner");
              default -> call;
            });
  }
}
