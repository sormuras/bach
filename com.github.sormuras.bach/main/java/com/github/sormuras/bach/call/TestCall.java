package com.github.sormuras.bach.call;

import java.io.PrintWriter;
import java.util.List;
import java.util.spi.ToolProvider;

public record TestCall(ToolProvider provider, List<String> arguments)
    implements CallWith<TestCall>, ToolProvider {

  public static TestCall of(ToolProvider provider) {
    return new TestCall(provider, List.of());
  }

  @Override
  public TestCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new TestCall(provider, arguments);
  }

  @Override
  public String toDescription(int maxLineLength) {
    var tool = provider.getClass();
    return tool.getModule().getDescriptor().toNameAndVersion()
        + '/'
        + tool.getName()
        + ' '
        + String.join(" ", arguments);
  }

  @Override
  public String name() {
    return "test";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    Thread.currentThread().setContextClassLoader(provider.getClass().getClassLoader());
    return provider.run(out, err, args);
  }
}
