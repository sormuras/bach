package project;

import run.bach.ToolCall;

public class CompileModules extends run.bach.project.workflow.CompileModules {
  @Override
  protected ToolCall createJarCall() {
    return ToolCall.of("jar", "--verbose");
  }
}
