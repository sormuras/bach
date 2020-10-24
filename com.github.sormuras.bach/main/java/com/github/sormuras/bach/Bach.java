package com.github.sormuras.bach;

import java.io.PrintStream;

/** Java Shell Builder. */
public final class Bach implements Print {

  /**
   * Returns a shell builder initialized with default components.
   *
   * @return a new instance of {@code Bach} initialized with default components
   */
  public static Bach ofSystem() {
    return new Bach(System.out);
  }

  /**
   * Returns the version of Bach.
   *
   * @return the version as a string or {@code "?"} if the version is unknown at runtime
   */
  public static String version() {
    var descriptor = Bach.class.getModule().getDescriptor();
    if (descriptor == null) return "?";
    return descriptor.version().map(Object::toString).orElse("?");
  }

  /** A print stream for normal messages. */
  private final PrintStream printer;

  /**
   * Initialize default constructor.
   *
   * @param printer the print stream for normal messages
   */
  public Bach(PrintStream printer) {
    this.printer = printer;
  }

  /**
   * Returns the print stream for normal messages.
   *
   * @return the print stream for normal messages
   */
  public PrintStream printer() {
    return printer;
  }
}
