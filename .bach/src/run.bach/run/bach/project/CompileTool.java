package run.bach.project;

import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;

public class CompileTool implements ToolOperator {

  static final String NAME = "compile";

  public CompileTool() {}

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
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
      bach.run(
          CompileClassesTool.NAME, space.name()); // translate Java source files into class files
      bach.run(
          CompileModulesTool.NAME, space.name()); // archive compiled classes in modular JAR files
    }
  }
}
