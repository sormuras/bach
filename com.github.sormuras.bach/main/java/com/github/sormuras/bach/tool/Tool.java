package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Command;
import java.util.List;

/** A nonimal tool command with arbitrary arguments. */
public record Tool(String name, List<Argument> arguments) implements Command<Tool> {

  @Override
  public Tool arguments(List<Argument> arguments) {
    if (this.arguments == arguments) return this;
    return new Tool(name, arguments);
  }
}
