package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolFinder;
import com.github.sormuras.bach.ToolRun;
import com.github.sormuras.bach.internal.ModuleFinderSupport;
import com.github.sormuras.bach.project.ProjectSpace;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;

public class RunAllTestsWorkflow extends AbstractSpaceWorkflow {

  public RunAllTestsWorkflow(Bach bach, Project project, ProjectSpace space) {
    super(bach, project, space);
  }

  protected ModuleFinder computeRuntimeModuleFinder() {
    var paths = new ArrayList<Path>();
    paths.add(computeOutputDirectoryForModules(space));
    paths.addAll(computeModulePathsOption().values());
    return ModuleFinder.of(paths.toArray(Path[]::new));
  }

  @Override
  public void run() {
    bach.logCaption("Run all tests in %s space".formatted(space.name()));
    var moduleFinder = computeRuntimeModuleFinder();
    for (var module : space.modules()) {
      var name = module.name();
      if (ModuleFinderSupport.findMainClass(moduleFinder, name).isPresent()) {
        new RunModuleWorkflow(bach, project, space, Command.of(name)).run();
      }
      var toolFinder = ToolFinder.of(moduleFinder, true, name);
      runTool(toolFinder, "test", bach.printer()::print);
      if (toolFinder.find("junit").isPresent()) {
        runJUnit(toolFinder, name);
      }
    }
  }

  private void runTool(ToolFinder finder, String tool, ToolRun.Visitor visitor) {
    for (var provider : finder.list(tool)) {
      var singleton = ToolFinder.of(provider);
      var call = ToolCall.of(singleton, Command.of(tool));
      var run = bach.run(call);
      visitor.accept(run);
    }
  }

  private void runJUnit(ToolFinder finder, String module) {
    runJUnit(finder, module, ToolRun.Visitor.noop());
  }

  private void runJUnit(ToolFinder finder, String module, ToolRun.Visitor visitor) {
    var reports = computeOutputDirectoryForReports().resolve("junit").resolve(module);
    var junit = Command.junit().add("--select-module", module).add("--reports-dir", reports);
    var call = ToolCall.of(finder, junit);
    var run = bach.run(call);
    visitor.accept(run);
  }
}
