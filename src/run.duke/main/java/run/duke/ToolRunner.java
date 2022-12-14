package run.duke;

import java.util.spi.ToolProvider;
import run.duke.internal.NullToolRunner;

@FunctionalInterface
public interface ToolRunner extends ToolTrait {
  static ToolRunner nullRunner() {
    return NullToolRunner.INSTANCE;
  }

  void run(ToolProvider provider, String... args);

  default ToolFinders toolFinders() {
    return ToolFinders.EMPTY;
  }

  @Override
  default ToolRunner toolRunner() {
    return this;
  }
}
