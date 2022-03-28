package com.github.sormuras.bach.tools;

import java.util.List;

public record TestToolProvider() implements BachToolProvider {
  @Override
  public String name() {
    return "test";
  }

  @Override
  public int run(BachAPI bach, List<String> arguments) {
    bach.banner("TODO: test " + System.identityHashCode(bach.project()));
    return 0;
  }
}
