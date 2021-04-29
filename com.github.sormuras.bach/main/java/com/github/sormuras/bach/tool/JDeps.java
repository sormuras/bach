package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Command;
import java.util.List;

public record JDeps(List<String> arguments) implements Command<JDeps> {

  public JDeps() {
    this(List.of());
  }

  @Override
  public String name() {
    return "jdeps";
  }

  @Override
  public JDeps arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new JDeps(arguments);
  }
}
