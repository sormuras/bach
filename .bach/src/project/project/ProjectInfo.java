package project;

import java.time.ZonedDateTime;
import run.bach.Bach;
import run.bach.project.Project;

public class ProjectInfo implements run.bach.project.ProjectComposer {
  @Override
  public Project composeProject(Project project) {
    return project
        .withName("Bach")
        .withVersion(Bach.VERSION)
        .withVersionDate(ZonedDateTime.now().toString())
        .withTargetsJava("17")
        .withRequiresModule("java.base")
        .withModule("main", ".bach/src/run.bach", ".bach/src/run.bach/module-info.java")
        .withLauncher("run.bach/run.bach.Main");
  }
}
