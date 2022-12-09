package run.bach.tool;

import java.io.PrintWriter;
import run.bach.Project;
import run.bach.ProjectTool;
import run.bach.ProjectToolRunner;
import run.duke.ToolCall;

public class CompileTool extends ProjectTool {
  public static ToolCall compile() {
    return ToolCall.of("compile");
  }

  public CompileTool(Project project, ProjectToolRunner runner) {
    super("compile", project, runner);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    for (var space : project().spaces()) {
      var modules = space.modules().list();
      if (modules.isEmpty()) {
        if (options().verbose()) {
          info("No modules declared in %s space.".formatted(space.name()));
        }
        continue;
      }
      var s = modules.size() == 1 ? "" : "s";
      info("Compile %d module%s in %s space...".formatted(modules.size(), s, space.name()));
      // translate Java source files into class files
      run(CompileClassesTool.compile(space));
      // archive compiled classes in modular JAR files
      run(CompileModulesTool.compile(space));
    }
    return 0;
  }
}
