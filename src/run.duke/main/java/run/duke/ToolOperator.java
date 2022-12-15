package run.duke;

import java.util.spi.ToolProvider;

@FunctionalInterface
public interface ToolOperator {
  default String name() {
    return getClass().getSimpleName();
  }

  ToolProvider provider(Workbench workbench);
}
