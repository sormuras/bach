package com.github.sormuras.bach.command;

import com.github.sormuras.bach.Command;

/**
 * The javadoc command generates HTML pages of API documentation from Java source files.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/man/javadoc.html">javadoc</a>
 */
public record JavadocCommand(AdditionalArgumentsOption additionals)
    implements Command<JavadocCommand> {

  public JavadocCommand() {
    this(AdditionalArgumentsOption.empty());
  }

  @Override
  public String name() {
    return "javadoc";
  }

  @Override
  public JavadocCommand additionals(AdditionalArgumentsOption additionals) {
    return new JavadocCommand(additionals);
  }
}
