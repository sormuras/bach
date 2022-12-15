package project;

import java.util.spi.ToolProvider;
import run.bach.tool.CompileClassesTool;
import run.duke.ToolCall;
import run.duke.Workbench;

public final class CompileClasses extends CompileClassesTool {
  public CompileClasses() {}

  private CompileClasses(Workbench workbench) {
    super(workbench);
  }

  @Override
  public ToolProvider provider(Workbench workbench) {
    return new CompileClasses(workbench);
  }

  @Override
  protected ToolCall createJavacCall() {
    return super.createJavacCall().with("-X" + "lint:all", "-W" + "error");
  }
}
