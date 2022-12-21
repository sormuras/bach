package run.duke;

import run.duke.internal.InoperativeWorkbench;

public interface Workbench extends ToolFinder, ToolRunner {
  static Workbench inoperative() {
    return new InoperativeWorkbench();
  }

  @Override
  default Workbench workbench() {
    return this;
  }

  void run(ToolCall call);

  Toolbox toolbox();

  <T> T workpiece(Class<T> type);
}
