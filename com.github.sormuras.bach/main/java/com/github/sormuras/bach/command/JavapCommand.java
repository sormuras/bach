package com.github.sormuras.bach.command;

import com.github.sormuras.bach.Command;

/**
 * The javap command disassembles one or more class files.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/man/javap.html">javap</a>
 */
public record JavapCommand(AdditionalArgumentsOption additionals) implements Command<JavapCommand> {
  public JavapCommand() {
    this(AdditionalArgumentsOption.empty());
  }

  @Override
  public String name() {
    return "javap";
  }

  @Override
  public JavapCommand additionals(AdditionalArgumentsOption additionals) {
    return new JavapCommand(additionals);
  }
}
