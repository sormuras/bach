package run.bach.tool;

import java.io.PrintWriter;
import run.bach.Bach;
import run.bach.Project;
import run.bach.internal.ModulesSupport;
import run.duke.ToolCall;

public class CacheTool implements Bach.Operator {
  public static ToolCall cache() {
    return ToolCall.of("cache");
  }

  public CacheTool() {}

  @Override
  public final String name() {
    return "cache";
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var finders =
        bach.project().spaces().list().stream()
            .map(Project.Space::modules)
            .map(Project.DeclaredModules::toModuleFinder)
            .toList();
    var missing = ModulesSupport.listMissingNames(finders, bach.project().externals().requires());
    if (missing.isEmpty()) return 0;
    bach.run("load", load -> load.with("modules").with(missing.stream().sorted()));
    return 0;
  }
}
