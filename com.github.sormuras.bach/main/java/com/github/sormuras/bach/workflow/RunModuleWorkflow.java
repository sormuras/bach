package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolRun;
import com.github.sormuras.bach.project.ProjectSpace;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;

public class RunModuleWorkflow extends AbstractSpaceWorkflow {

  private final Command<?> command;

  public RunModuleWorkflow(Bach bach, Project project, ProjectSpace space, Command<?> command) {
    super(bach, project, space);
    this.command = command;
  }

  protected ModuleFinder computeRuntimeModuleFinder() {
    var paths = new ArrayList<Path>();
    paths.add(computeOutputDirectoryForModules(space));
    paths.addAll(computeModulePathsOption().values());
    return ModuleFinder.of(paths.toArray(Path[]::new));
  }

  protected ToolCall computeToolCall() {
    var finder = computeRuntimeModuleFinder();
    return ToolCall.module(finder, command);
  }

  protected void visitToolRun(ToolRun run) {
    bach.printer().print(run);
  }

  @Override
  public void run() {
    var call = computeToolCall();
    var run = bach.run(call);
    visitToolRun(run);
  }
}
