package com.github.sormuras.bach.call;

import java.io.PrintWriter;
import java.util.List;
import java.util.spi.ToolProvider;

public record JUnitCall(ToolProvider provider, List<String> arguments)
    implements CallWith<JUnitCall>, ToolProvider {

  public static JUnitCall of(ToolProvider provider) {
    return new JUnitCall(provider, List.of());
  }

  @Override
  public JUnitCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new JUnitCall(provider, arguments);
  }

  @Override
  public String name() {
    return "junit";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    Thread.currentThread().setContextClassLoader(provider.getClass().getClassLoader());
    return provider.run(out, err, args);
  }
}
