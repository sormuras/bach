package run.bach.tool;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import run.bach.Project;
import run.bach.ProjectTool;
import run.bach.internal.ModulesSupport;
import run.duke.ToolCall;
import run.duke.Workbench;

public class CacheTool extends ProjectTool {
  public static ToolCall cache() {
    return ToolCall.of("cache");
  }

  public CacheTool() {}

  protected CacheTool(Workbench workbench) {
    super(workbench);
  }

  @Override
  public final String name() {
    return "cache";
  }

  @Override
  public ToolProvider provider(Workbench workbench) {
    return new CacheTool(workbench);
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
    run("load", load -> load.with("modules").with(missing.stream().sorted()));
    return 0;
  }
}
