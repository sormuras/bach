package run.duke;

import java.util.spi.ToolProvider;

public interface Tooling extends ToolFinder, ToolRunner, ToolOperator, ToolProvider {
  @Override
  String name();
}
