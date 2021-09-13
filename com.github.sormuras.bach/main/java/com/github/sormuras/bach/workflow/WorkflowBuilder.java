package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.command.Composer;
import com.github.sormuras.bach.command.DefaultCommand;
import com.github.sormuras.bach.project.ProjectSpace;

/** A workflow-based builder of projects. */
public record WorkflowBuilder(Bach bach, Project project) {

  public void compile() {
    for (var space : project.spaces()) compile(space);
  }

  public void compile(ProjectSpace space) {
    runWorkflow(new CompileWorkflow(bach, project, space));
  }

  public void runModule(String module, Object... arguments) {
    var space = project.spaces().values().get(0);
    runModule(space, module, command -> command.addAll(arguments));
  }

  public void runModule(ProjectSpace space, String module, Composer<DefaultCommand> composer) {
    var command = composer.apply(Command.of(module));
    runWorkflow(new RunModuleWorkflow(bach, project, space, command));
  }

  public void runWorkflow(Workflow... workflows) {
    for (var workflow : workflows) workflow.run();
  }
}
