package run.duke;

import java.util.List;
import java.util.spi.ToolProvider;
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

  void run(ToolProvider provider, List<String> arguments);

  Toolbox toolbox();

  <T> T workpiece(Class<T> type);
}
