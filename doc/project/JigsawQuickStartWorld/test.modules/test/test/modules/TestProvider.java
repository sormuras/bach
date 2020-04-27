package test.modules;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import org.astro.World;

public class TestProvider implements ToolProvider {

  @Override
  public String name() {
    return "test(test.modules)";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    assert "test.modules".equals(getClass().getModule().getName());
    assert "org.astro".equals(World.class.getModule().getName());
    return 0;
  }
}
