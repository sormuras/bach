package run.bach.tool;

import run.bach.ProjectOperator;
import run.bach.ProjectRunner;
import run.duke.ToolCall;
import run.duke.ToolLogger;

public class CompileTool implements ProjectOperator {
  public static ToolCall compile() {
    return ToolCall.of("compile");
  }

  public CompileTool() {}

  @Override
  public final String name() {
    return "compile";
  }

  @Override
  public void run(ProjectRunner runner, ToolLogger logger, String... args) {
    for (var space : runner.project().spaces()) {
      var modules = space.modules().list();
      if (modules.isEmpty()) {
        logger.debug("No modules declared in %s space.".formatted(space.name()));
        continue;
      }
      var s = modules.size() == 1 ? "" : "s";
      logger.log("Compile %d module%s in %s space...".formatted(modules.size(), s, space.name()));
      // translate Java source files into class files
      runner.run(CompileClassesTool.compile(space));
      // archive compiled classes in modular JAR files
      runner.run(CompileModulesTool.compile(space));
    }
  }
}
