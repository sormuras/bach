package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Command;
import java.util.List;

public record Java(List<String> arguments) implements Command<Java> {

  public Java() {
    this(List.of());
  }

  @Override
  public String name() {
    return "java";
  }

  @Override
  public Java arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new Java(arguments);
  }
}
