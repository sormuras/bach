package run.bach;

import java.util.Optional;
import java.util.spi.ToolProvider;
import run.duke.Tool;
import run.duke.ToolCall;

public abstract class ProjectTool implements ToolProvider, ProjectToolRunner {
  private final String name;
  private final ProjectToolRunner runner;

  public ProjectTool(String name, ProjectToolRunner runner) {
    this.name = name;
    this.runner = runner;
  }

  @Override
  public final String name() {
    return name;
  }

  public final Project project() {
    return runner.project();
  }

  @Override
  public final Options options() {
    return runner.options();
  }

  @Override
  public final Folders folders() {
    return runner.folders();
  }

  @Override
  public final Printer printer() {
    return runner.printer();
  }

  public Optional<? extends Tool> find(String string) {
    return runner.toolFinders().findTool(string);
  }

  @Override
  public void run(ToolCall call) {
    runner.run(call);
  }

  @Override
  public void run(ToolProvider provider, String... args) {
    runner.run(provider, args);
  }

  public final void info(Object message) {
    runner.printer().log(System.Logger.Level.INFO, message);
  }
}
