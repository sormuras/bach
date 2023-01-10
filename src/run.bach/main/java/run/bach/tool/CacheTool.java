package run.bach.tool;

import run.bach.Project;
import run.bach.ProjectOperator;
import run.bach.ProjectRunner;
import run.bach.internal.ModulesSupport;
import run.duke.ToolCall;
import run.duke.ToolLogger;

public class CacheTool implements ProjectOperator {
  public static ToolCall cache() {
    return ToolCall.of("cache");
  }

  public CacheTool() {}

  @Override
  public final String name() {
    return "cache";
  }

  @Override
  public void run(ProjectRunner runner, ToolLogger logger, String... args) {
    var project = runner.project();
    var finders =
        project.spaces().list().stream()
            .map(Project.Space::modules)
            .map(Project.DeclaredModules::toModuleFinder)
            .toList();
    var missing = ModulesSupport.listMissingNames(finders, project.externals().requires());
    if (missing.isEmpty()) return;
    runner.run("load", load -> load.with("modules").with(missing.stream().sorted()));
  }
}
