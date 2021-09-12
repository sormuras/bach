package com.github.sormuras.bach.customizable;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.command.Composer;
import com.github.sormuras.bach.command.DefaultCommand;
import com.github.sormuras.bach.project.ProjectSpace;

/** A workflow-based builder of projects. */
public record CustomizableBuilder(Bach bach, Project project) {

  public void compile() {
    for (var space : project.spaces()) compile(space);
  }

  public void compile(ProjectSpace space) {
    runWorkflow(new CompileWorkflow(bach, project, space));
  }

  public void runModule(String module, Composer<DefaultCommand> composer) {
    runModule(project.spaces().values().get(0), module, composer);
  }

  public void runModule(ProjectSpace space, String module, Composer<DefaultCommand> composer) {
    // TODO runWorkflow(new RunModuleWorkflow(...));
  }

  public void runWorkflow(Workflow... workflows) {
    for (var workflow : workflows) workflow.run();
  }
}
