package run.duke;

import java.util.spi.ToolProvider;
import run.duke.internal.InoperativeWorkbench;

public interface Workbench extends ToolRunner {
  static Workbench inoperative() {
    return new InoperativeWorkbench();
  }

  @Override
  default Workbench workbench() {
    return this;
  }

  void run(ToolCall call, ToolProvider provider, String... args);

  Toolbox toolbox();

  <T> T workpiece(Class<T> type);
}
