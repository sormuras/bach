package run.bach;

import java.util.Optional;
import java.util.spi.ToolProvider;
import run.duke.Tool;
import run.duke.ToolOperator;
import run.duke.Workbench;

public abstract class ProjectTool implements ToolProvider, ToolOperator, BachRunner {
  private final Workbench workbench;

  public ProjectTool() {
    this(Workbench.inoperative());
  }

  protected ProjectTool(Workbench workbench) {
    this.workbench = workbench;
  }

  @Override
  public abstract String name();

  @Override
  public Workbench workbench() {
    return workbench;
  }

  public Optional<Tool> find(String tool) {
    return workbench.toolbox().find(tool);
  }

  public final void debug(Object message) {
    printer().log(System.Logger.Level.DEBUG, message);
  }

  public final void info(Object message) {
    printer().log(System.Logger.Level.INFO, message);
  }
}
