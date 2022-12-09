package project;

import run.bach.Project;
import run.bach.ProjectToolRunner;
import run.bach.tool.CompileClassesTool;
import run.duke.ToolCall;

public final class CompileClasses extends CompileClassesTool {
  public CompileClasses(Project project, ProjectToolRunner runner) {
    super(project, runner);
  }

  @Override
  protected ToolCall createJavacCall() {
    return super.createJavacCall().with("-X" + "lint:all", "-W" + "error");
  }
}
