package project;

import java.time.ZonedDateTime;
import run.bach.Bach;
import run.bach.Project;

public class Info implements Project.Composer {
  @Override
  public Project composeProject(Project project) {
    return Project.ofDefaults()
        .withVersion(Bach.VERSION)
        .withVersionDate(ZonedDateTime.now().toString())
        .withTargetsJava("17")
        .withRequiresModule("java.base")
        .withModule("main", "src/run.bach", "src/run.bach/module-info.java")
        .withLauncher("run.bach/run.bach.Main");
  }
}
