package project;

import run.bach.ToolCall;
import run.bach.project.workflow.CompileClasses;

public class CompileClassesJavacTweak implements CompileClasses.JavacTweak {
  @Override
  public ToolCall tweakWorkflowCompileClassesJavac(ToolCall call, TweakContext context) {
    var tweaked = call.with("-g").with("-parameters").with("-X" + "lint");
    if (context.space().name().equals("main")) {
      tweaked = tweaked.with("-W" + "error");
    }
    return tweaked;
  }
}
