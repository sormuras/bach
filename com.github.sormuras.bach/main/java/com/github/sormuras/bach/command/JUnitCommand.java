package com.github.sormuras.bach.command;

import com.github.sormuras.bach.Command;

/**
 * The junit command starts the JUnit Platform via its console launcher.
 *
 * @see <a
 *     href="https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher">junit</a>
 */
public record JUnitCommand(AdditionalArgumentsOption additionals) implements Command<JUnitCommand> {
  public JUnitCommand() {
    this(AdditionalArgumentsOption.empty());
  }

  @Override
  public String name() {
    return "junit";
  }

  @Override
  public JUnitCommand additionals(AdditionalArgumentsOption additionals) {
    return new JUnitCommand(additionals);
  }
}
