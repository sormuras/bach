package com.github.sormuras.bach.tools;

import java.util.List;

public record BuildToolProvider() implements BachToolProvider {
  @Override
  public String name() {
    return "build";
  }

  @Override
  public int run(BachAPI bach, List<String> arguments) {
    bach.banner("BUILD " + System.identityHashCode(bach.project()));
    new CompileToolProvider().run(bach, List.of());
    new TestToolProvider().run(bach, List.of());
    return 0;
  }
}
