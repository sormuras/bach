package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Command;
import java.util.List;

public record JDeps(List<Argument> arguments) implements Command<JDeps> {

  public JDeps() {
    this(List.of());
  }

  @Override
  public String name() {
    return "jdeps";
  }

  @Override
  public JDeps arguments(List<Argument> arguments) {
    return new JDeps(arguments);
  }
}
