package run.bach.project;

import run.bach.ToolOperator;

public class CompileTool implements ToolOperator {
  public CompileTool() {}

  @Override
  public String name() {
    return "compile";
  }

  @Override
  public void run(Operation operation) {
    var bach = operation.bach();
    for (var space : bach.project().spaces().list()) {
      var modules = space.modules().list();
      if (modules.isEmpty()) {
        if (bach.cli().verbose()) {
          bach.info("No modules declared in %s space.".formatted(space.name()));
        }
        continue;
      }
      var s = modules.size() == 1 ? "" : "s";
      bach.info("Compile %d module%s in %s space...".formatted(modules.size(), s, space.name()));
      // translate Java source files into class files
      bach.run(CompileClassesTool.class, space.name());
      // archive compiled classes in modular JAR files
      bach.run(CompileModulesTool.class, space.name());
    }
  }
}
