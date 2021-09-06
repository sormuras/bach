package com.github.sormuras.bach.command;

import com.github.sormuras.bach.Command;

/**
 * A command implementation for an arbitrary tool name taking arbitrary arguments.
 *
 * @param name The name of the command.
 * @param additionals Aggregates additional command-line arguments.
 */
public record DefaultCommand(String name, AdditionalArgumentsOption additionals)
    implements Command<DefaultCommand> {

  public DefaultCommand(String name) {
    this(name, AdditionalArgumentsOption.empty());
  }

  @Override
  public DefaultCommand additionals(AdditionalArgumentsOption additionals) {
    return new DefaultCommand(name, additionals);
  }
}
