package com.github.sormuras.bach;

import java.util.List;

/** A nonimal tool command with arbitrary arguments. */
public record Tool(String name, List<String> arguments) implements Command<Tool> {

  @Override
  public Tool arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new Tool(name, arguments);
  }
}