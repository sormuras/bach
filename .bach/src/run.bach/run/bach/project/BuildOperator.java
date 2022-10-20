package run.bach.project;

import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;

public class BuildOperator implements ToolOperator {

  static final String NAME = "build";

  public BuildOperator() {}

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    bach.run(CacheOperator.NAME); // go offline and verify cached assets
    bach.run(CompileOperator.NAME); // compile all modules spaces
    bach.run(TestOperator.NAME); // start launcher and execute testables in test space
  }
}
