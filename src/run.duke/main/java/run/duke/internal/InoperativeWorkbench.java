package run.duke.internal;

import run.duke.ToolCall;
import run.duke.Toolbox;
import run.duke.Workbench;
import run.duke.Workpieces;

public record InoperativeWorkbench(Workpieces workpieces) implements Workbench {
  public InoperativeWorkbench() {
    this(new Workpieces());
  }

  @Override
  public void run(ToolCall call) {}

  @Override
  public Toolbox toolbox() {
    return Toolbox.empty();
  }

  @Override
  public <T> T workpiece(Class<T> type) {
    return workpieces.get(type);
  }
}
