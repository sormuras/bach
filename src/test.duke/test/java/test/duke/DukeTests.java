package test.duke;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.spi.ToolProvider;
import jdk.jfr.Enabled;
import jdk.jfr.Registered;
import run.duke.Duke;

@Registered
@Enabled
public class DukeTests implements ToolProvider {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    assert "duke list tools".equals(Duke.listTools().toCommandLine());
    assert "duke tree --mode=DELETE .".equals(Duke.treeDelete(Path.of(".")).toCommandLine());
    return 0;
  }
}
