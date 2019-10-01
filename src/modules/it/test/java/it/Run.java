package it;

import java.io.PrintWriter;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;

public class Run implements ToolProvider {
  @Override
  public String name() {
    return "test(it)";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    // Requires JUnit Platform Code 1.6.0-SNAPSHOT or later
    var junit =
        ServiceLoader.load(ToolProvider.class, getClass().getClassLoader()).stream()
            .map(ServiceLoader.Provider::get)
            .filter(provider -> provider.name().equals("junit"))
            .findFirst()
            .orElseThrow();
    return junit.run(out, err, "--select-module", "it");
  }
}
