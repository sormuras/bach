package com.github.sormuras.bach.command;

import com.github.sormuras.bach.Command;

/**
 * The jlink command links a set of modules, along with their transitive dependences, to create a
 * custom runtime image.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/man/jlink.html">jlink</a>
 */
public record JLinkCommand(AdditionalArgumentsOption additionals) implements Command<JLinkCommand> {
  public JLinkCommand() {
    this(AdditionalArgumentsOption.empty());
  }

  @Override
  public String name() {
    return "jlink";
  }

  @Override
  public JLinkCommand additionals(AdditionalArgumentsOption additionals) {
    return new JLinkCommand(additionals);
  }
}
