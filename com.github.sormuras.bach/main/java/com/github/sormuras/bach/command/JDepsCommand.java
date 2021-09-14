package com.github.sormuras.bach.command;

import com.github.sormuras.bach.Command;

/**
 * The jdeps command shows the package-level or class-level dependencies of Java class files.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/man/jdeps.html">jdeps</a>
 */
public record JDepsCommand(AdditionalArgumentsOption additionals) implements Command<JDepsCommand> {
  public JDepsCommand() {
    this(AdditionalArgumentsOption.empty());
  }

  @Override
  public String name() {
    return "jdeps";
  }

  @Override
  public JDepsCommand additionals(AdditionalArgumentsOption additionals) {
    return new JDepsCommand(additionals);
  }
}
