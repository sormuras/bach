package test.duke;

import run.duke.ToolCall;
import run.duke.Toolbox;
import run.duke.Workbench;
import run.duke.Workpieces;

public record MockWorkbench(Toolbox toolbox, Workpieces workpieces) implements Workbench {
  @Override
  public void run(ToolCall call) {}

  @Override
  public <T> T workpiece(Class<T> type) {
    return workpieces.get(type);
  }
}
