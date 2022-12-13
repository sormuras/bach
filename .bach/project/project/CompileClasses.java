package project;

import run.bach.ProjectToolRunner;
import run.bach.tool.CompileClassesTool;
import run.duke.ToolCall;

public final class CompileClasses extends CompileClassesTool {
  public CompileClasses(ProjectToolRunner runner) {
    super(runner);
  }

  @Override
  protected ToolCall createJavacCall() {
    return super.createJavacCall().with("-X" + "lint:all", "-W" + "error");
  }
}
