package project;

import run.bach.ToolCall;
import run.bach.project.CompileClassesTool;

public class CompileClasses extends CompileClassesTool {
  @Override
  protected ToolCall javacWithDestinationDirectory(ToolCall javac, OperationContext context) {
    javac = javac.with("-deprecation"); // Output source locations where deprecated APIs are used
    javac = javac.with("-g"); // Generate all debugging info
    javac = javac.with("-X" + "lint"); // Enable recommended warnings
    javac = javac.with("-W" + "error"); // Terminate compilation if warnings occur
    return super.javacWithDestinationDirectory(javac, context);
  }
}
