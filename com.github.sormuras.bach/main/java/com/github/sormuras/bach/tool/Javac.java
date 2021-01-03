package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Command;
import java.util.List;

public record Javac(List<Argument> arguments) implements Command<Javac> {

  public Javac() {
    this(List.of());
  }

  @Override
  public String name() {
    return "javac";
  }

  @Override
  public Javac arguments(List<Argument> arguments) {
    return new Javac(arguments);
  }
}
