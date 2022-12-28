package run.bach.tool;

import java.io.PrintWriter;
import run.bach.Bach;
import run.duke.ToolCall;

public class CompileTool implements Bach.Operator {
  public static ToolCall compile() {
    return ToolCall.of("compile");
  }

  public CompileTool() {}

  @Override
  public final String name() {
    return "compile";
  }

  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    for (var space : bach.project().spaces()) {
      var modules = space.modules().list();
      if (modules.isEmpty()) {
        if (bach.options().verbose()) {
          out.println("No modules declared in %s space.".formatted(space.name()));
        }
        continue;
      }
      var s = modules.size() == 1 ? "" : "s";
      out.println("Compile %d module%s in %s space...".formatted(modules.size(), s, space.name()));
      // translate Java source files into class files
      bach.run(CompileClassesTool.compile(space));
      // archive compiled classes in modular JAR files
      bach.run(CompileModulesTool.compile(space));
    }
    return 0;
  }
}
