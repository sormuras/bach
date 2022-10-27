package project;

import java.time.ZonedDateTime;
import run.bach.Bach;
import run.bach.Project;

public class Info implements Project.Composer {
  @Override
  public Project composeProject(Project project) {
    return project
        .withVersion(Bach.VERSION)
        .withVersionDate(ZonedDateTime.now().toString())
        .withTargetsJava("17")
        .withRequiresModule("java.base")
        .withModule("main", ".bach/src/run.bach", ".bach/src/run.bach/module-info.java")
        .withModule("main", ".bach/project", ".bach/project/module-info.java")
        .withLauncher("run.bach/run.bach.Main");
  }
}
