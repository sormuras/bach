package run.bach;

import java.util.Optional;
import java.util.spi.ToolProvider;

import run.duke.Tool;
import run.duke.ToolCall;
import run.duke.ToolRunner;
import run.duke.Toolbox;

public abstract class ProjectOperator implements ToolProvider, Workbench {
  private final String name;
  private final Workbench workbench;
  private final Project project;

  public ProjectOperator(String name, Project project, Workbench workbench) {
    this.name = name;
    this.project = project;
    this.workbench = workbench;
  }

  @Override
  public final String name() {
    return name;
  }

  public final Project project() {
    return project;
  }

  public final Options options() {
    return workbench.options();
  }

  public final Folders folders() {
    return workbench.folders();
  }

  public final Printer printer() {
    return workbench.printer();
  }

  public final Toolbox toolbox() {
    return workbench.toolbox();
  }

  public final Optional<Tool> find(String tool) {
    return toolbox().find(tool, workbench);
  }

  @Override
  public void run(ToolCall call) {
    workbench.run(call);
  }

  public final void info(Object message) {
    workbench.printer().log(System.Logger.Level.INFO, message);
  }
}
