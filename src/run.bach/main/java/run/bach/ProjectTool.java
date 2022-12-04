package run.bach;

import java.util.Optional;
import java.util.spi.ToolProvider;
import run.duke.Tool;
import run.duke.ToolCall;
import run.duke.ToolFinders;

public abstract class ProjectTool implements ToolProvider, ProjectToolRunner {
  private final String name;
  private final Project project;
  private final ProjectToolRunner runner;

  public ProjectTool(String name, Project project, ProjectToolRunner runner) {
    this.name = name;
    this.project = project;
    this.runner = runner;
  }

  @Override
  public final String name() {
    return name;
  }

  public final Project project() {
    return project;
  }

  public final Options options() {
    return runner.options();
  }

  public final Folders folders() {
    return runner.folders();
  }

  public final Printer printer() {
    return runner.printer();
  }

  public final ToolFinders finders() {
    return runner.finders();
  }

  public final Optional<Tool> find(String tool) {
    return toolbox().finders().find(tool, runner);
  }

  @Override
  public void run(ToolCall call) {
    runner.run(call);
  }

  public final void info(Object message) {
    runner.printer().log(System.Logger.Level.INFO, message);
  }
}
