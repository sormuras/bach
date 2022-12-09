package run.duke;

import java.util.spi.ToolProvider;

public interface ToolOperator extends ToolProvider {
  ToolRunner runner();
}
