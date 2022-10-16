package run.bach.project.workflow;

import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;

public class Build implements ToolOperator {

  static final String NAME = "build";

  public Build() {}

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    bach.run(Cache.NAME); // go offline and verify cached assets
    bach.run(Compile.NAME); // compile all modules spaces
    bach.run(Test.NAME); // start launcher and execute testables in test space
  }
}
