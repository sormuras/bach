package run.bach.tool;

import run.bach.Project;
import run.bach.ProjectTool;
import run.bach.Workbench;

import java.io.PrintWriter;

public class CompileTool extends ProjectTool {
  public static final String NAME = "compile";

  public CompileTool(Project project, Workbench workbench) {
    super(NAME, project, workbench);
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
      run(CompileClassesTool.NAME, space.name());
      // archive compiled classes in modular JAR files
      run(CompileModulesTool.NAME, space.name());
    }
    return 0;
  }

}
