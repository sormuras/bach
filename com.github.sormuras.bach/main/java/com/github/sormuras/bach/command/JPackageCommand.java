package com.github.sormuras.bach.command;

import com.github.sormuras.bach.Command;

/**
 * The jpackage command creates self-contained Java applications.
 *
 * <p>The jpackage tool will take as input a Java application and a Java run-time image, and produce
 * a Java application image that includes all the necessary dependencies. It will be able to produce
 * a native package in a platform-specific format, such as an exe on Windows or a dmg on macOS. Each
 * format must be built on the platform it runs on, there is no cross-platform support. The tool
 * will have options that allow packaged applications to be customized in various ways.
 *
 * @see <a
 *     href="https://docs.oracle.com/en/java/javase/16/docs/specs/man/jpackage.html">jpackage</a>
 */
public record JPackageCommand(AdditionalArgumentsOption additionals)
    implements Command<JPackageCommand> {
  public JPackageCommand() {
    this(AdditionalArgumentsOption.empty());
  }

  @Override
  public String name() {
    return "jmod";
  }

  @Override
  public JPackageCommand additionals(AdditionalArgumentsOption additionals) {
    return new JPackageCommand(additionals);
  }
}
