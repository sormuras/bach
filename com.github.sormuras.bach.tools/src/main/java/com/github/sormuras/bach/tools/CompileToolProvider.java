package com.github.sormuras.bach.tools;

import java.util.List;

public record CompileToolProvider() implements BachToolProvider {
  @Override
  public String name() {
    return "compile";
  }

  @Override
  public int run(BachAPI bach, List<String> arguments) {
    bach.run(Command.of("javac").with("--version"));
    bach.run(Command.of("jar").with("--version"));
    return 0;
  }
}
