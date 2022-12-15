package run.duke.internal;

import java.util.spi.ToolProvider;
import run.duke.Toolbox;
import run.duke.Workbench;
import run.duke.Workpieces;

public record InoperativeWorkbench(Workpieces workpieces) implements Workbench {
  public InoperativeWorkbench() {
    this(new Workpieces());
  }

  @Override
  public void run(ToolProvider provider, String... args) {}

  @Override
  public Toolbox toolbox() {
    return Toolbox.empty();
  }

  @Override
  public <T> T workpiece(Class<T> type) {
    return workpieces.get(type);
  }
}
