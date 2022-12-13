package run.duke.internal;

import java.util.spi.ToolProvider;
import run.duke.ToolRunner;

public record NullToolRunner() implements ToolRunner {
  public static final ToolRunner INSTANCE = new NullToolRunner();

  @Override
  public void run(ToolProvider provider, String... args) {}
}
