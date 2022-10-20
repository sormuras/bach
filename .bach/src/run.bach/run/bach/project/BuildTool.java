package run.bach.project;

import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;

public class BuildTool implements ToolOperator {

  static final String NAME = "build";

  public BuildTool() {}

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    bach.run(CacheTool.NAME); // go offline and verify cached assets
    bach.run(CompileTool.NAME); // compile all modules spaces
    bach.run(TestTool.NAME); // start launcher and execute testables in test space
  }
}
