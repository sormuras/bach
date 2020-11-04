package com.github.sormuras.bach;

import static java.util.Arrays.copyOfRange;

/** Bach's main program. */
public class Main {
  /**
   * Main entry-point.
   *
   * @param args the command line arguments
   */
  public static void main(String... args) {
    if (args.length == 0) {
      System.out.println("No argument, no action.");
      return;
    }
    switch (args[0]) {
      case "build" -> BuildProgram.build(copyOfRange(args, 1, args.length));
      case "help", "usage" -> System.out.println("Usage: bach ACTION [ARGS...]");
      default -> System.out.println("Unsupported action: " + args[0]);
    }
  }

  /** Hidden default constructor. */
  private Main() {}
}
