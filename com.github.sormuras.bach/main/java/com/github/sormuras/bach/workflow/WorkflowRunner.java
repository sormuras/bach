package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.command.Composer;
import com.github.sormuras.bach.command.DefaultCommand;
import com.github.sormuras.bach.project.ProjectSpace;

/** A workflow-running builder of projects. */
public record WorkflowRunner(Bach bach, Project project) {

  public void grabExternals() {
    run(new GrabExternalsWorkflow(bach, project));
  }

  public void compileSpaces() {
    for (var space : project.spaces()) runCompileWorkflow(space);
  }

  public void executeTests() {
    var testSpace = project.spaces().find("test");
    testSpace.ifPresent(this::runExecuteTestsWorkflow);
  }

  public void launchModule(String module, Object... arguments) {
    var space = project.spaces().values().get(0);
    runLaunchModuleWorkflow(space, module, command -> command.addAll(arguments));
  }

  public void run(Workflow... workflows) {
    for (var workflow : workflows) workflow.run();
  }

  public void runCompileWorkflow(ProjectSpace space) {
    run(new CompileWorkflow(bach, project, space));
  }

  public void runExecuteTestsWorkflow(ProjectSpace space) {
    run(new ExecuteTestsWorkflow(bach, project, space));
  }

  public void runLaunchModuleWorkflow(
      ProjectSpace space, String module, Composer<DefaultCommand> composer) {
    var command = composer.apply(Command.of(module));
    run(new LaunchModuleWorkflow(bach, project, space, command));
  }
}
