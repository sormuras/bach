package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Command;
import java.util.List;

public record Javadoc(List<String> arguments) implements Command<Javadoc> {

  public Javadoc() {
    this(List.of());
  }

  @Override
  public String name() {
    return "javadoc";
  }

  @Override
  public Javadoc arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new Javadoc(arguments);
  }
}
