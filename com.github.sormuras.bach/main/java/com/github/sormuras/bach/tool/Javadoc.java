package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Command;
import java.util.List;

public record Javadoc(List<Argument> arguments) implements Command<Javadoc> {

  public Javadoc {}

  public Javadoc() {
    this(List.of());
  }

  @Override
  public String name() {
    return "javadoc";
  }

  @Override
  public Javadoc arguments(List<Argument> arguments) {
    return new Javadoc(arguments);
  }
}
