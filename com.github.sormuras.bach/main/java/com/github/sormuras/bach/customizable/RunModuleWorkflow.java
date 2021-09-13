package com.github.sormuras.bach.customizable;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolRun;
import com.github.sormuras.bach.command.ModulePathsOption;
import com.github.sormuras.bach.project.ProjectSpace;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class RunModuleWorkflow extends Workflow {

  protected final ProjectSpace space;
  private final Command<?> command;

  public RunModuleWorkflow(Bach bach, Project project, ProjectSpace space, Command<?> command) {
    super(bach, project);
    this.space = space;
    this.command = command;
  }

  protected Path computeOutputDirectoryForModules(ProjectSpace space) {
    return bach.path().workspace(space.name(), "modules");
  }

  protected ModulePathsOption computeModulePathsOption() {
    var paths = ModulePathsOption.empty();
    for (var parent : space.parents()) paths = paths.add(computeOutputDirectoryForModules(parent));
    var externalModules = bach.path().externalModules();
    if (Files.isDirectory(externalModules)) paths = paths.add(externalModules);
    return paths;
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
