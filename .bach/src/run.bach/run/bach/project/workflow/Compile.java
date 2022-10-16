package run.bach.project.workflow;

import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;

public class Compile implements ToolOperator {

  static final String NAME = "compile";

  public Compile() {}

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    for (var space : bach.project().spaces().list()) {
      var modules = space.modules().list();
      if (modules.isEmpty()) {
        if (bach.configuration().cli().verbose()) {
          bach.info("No modules declared in %s space.".formatted(space.name()));
        }
        continue;
      }
      var s = modules.size() == 1 ? "" : "s";
      bach.info("Compile %d module%s in %s space...".formatted(modules.size(), s, space.name()));
      bach.run(CompileClasses.NAME, space.name()); // translate Java source files into class files
      bach.run(CompileModules.NAME, space.name()); // archive compiled classes in modular JAR files
    }
  }
}
