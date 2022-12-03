package run.bach.tool;

import java.io.PrintWriter;

import run.bach.Project;
import run.bach.ProjectTool;
import run.bach.ProjectToolRunner;
import run.bach.internal.ModulesSupport;

public class CacheTool extends ProjectTool {
  public static final String NAME = "cache";

  public CacheTool(Project project, ProjectToolRunner runner) {
    super(NAME, project, runner);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var finders =
        project().spaces().list().stream()
            .map(Project.Space::modules)
            .map(Project.DeclaredModules::toModuleFinder)
            .toList();
    var missing = ModulesSupport.listMissingNames(finders, project().externals().requires());
    if (missing.isEmpty()) return 0;
    // TODO run(LoadTool, load -> load.with("modules").with(missing.stream().sorted()));
    printer().log(System.Logger.Level.ERROR, "Not implemented, yet");
    return 1;
  }
}
