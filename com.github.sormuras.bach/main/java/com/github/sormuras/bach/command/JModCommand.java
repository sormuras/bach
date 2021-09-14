package com.github.sormuras.bach.command;

import com.github.sormuras.bach.Command;

/**
 * The jmod command is intended for modules that have native libraries or other configuration files
 * or for modules that you intend to link, with the jlink tool, to a runtime image.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/man/jmod.html">jmod</a>
 */
public record JModCommand(AdditionalArgumentsOption additionals) implements Command<JModCommand> {
  public JModCommand() {
    this(AdditionalArgumentsOption.empty());
  }

  @Override
  public String name() {
    return "jmod";
  }

  @Override
  public JModCommand additionals(AdditionalArgumentsOption additionals) {
    return new JModCommand(additionals);
  }
}
