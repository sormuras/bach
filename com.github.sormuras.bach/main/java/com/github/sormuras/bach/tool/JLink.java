package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Command;
import java.util.List;

public record JLink(List<String> arguments) implements Command<JLink> {

  public JLink() {
    this(List.of());
  }

  @Override
  public String name() {
    return "jlink";
  }

  @Override
  public JLink arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new JLink(arguments);
  }
}
