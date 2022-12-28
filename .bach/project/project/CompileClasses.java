package project;

import run.bach.tool.CompileClassesTool;
import run.duke.ToolCall;

public final class CompileClasses extends CompileClassesTool {
  @Override
  protected ToolCall createJavacCall() {
    return super.createJavacCall().with("-X" + "lint:all").with("-W" + "error");
  }
}
