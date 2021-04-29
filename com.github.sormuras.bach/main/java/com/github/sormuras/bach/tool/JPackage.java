package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Command;
import java.util.List;

public record JPackage(List<String> arguments) implements Command<JPackage> {

  public JPackage() {
    this(List.of());
  }

  @Override
  public String name() {
    return "jpackage";
  }

  @Override
  public JPackage arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new JPackage(arguments);
  }
}
