package org.astro;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class TestProvider implements ToolProvider {

  @Override
  public String name() {
    return "test(org.astro)";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    assert "World".equals(new World().world);
    return 0;
  }
}
