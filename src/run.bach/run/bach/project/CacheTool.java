package run.bach.project;

import run.bach.Project;
import run.bach.ToolOperator;
import run.bach.internal.ModulesSupport;
import run.bach.toolbox.LoadTool;

public class CacheTool implements ToolOperator {
  public CacheTool() {}

  @Override
  public String name() {
    return "cache";
  }

  @Override
  public void run(Operation operation) {
    var project = operation.bach().project();
    var finders =
        project.spaces().list().stream()
            .map(Project.Space::modules)
            .map(Project.DeclaredModules::toModuleFinder)
            .toList();
    var missing = ModulesSupport.listMissingNames(finders, project.externals().requires());
    if (missing.isEmpty()) return;
    operation.run(LoadTool.class, load -> load.with("modules").with(missing.stream().sorted()));
  }
}
