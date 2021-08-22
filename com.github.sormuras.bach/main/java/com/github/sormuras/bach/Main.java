package com.github.sormuras.bach;

/** The main entry-point of Bach. */
class Main {
  public static void main(String... args) {
    if (args.length == 0) {
      printHelp();
      return;
    }
    var options = Options.parse(args);
    if (options.forMain().help().isPresent()) {
      printHelp();
      return;
    }
    if (options.forMain().version().isPresent()) {
      System.out.println(Bach.version());
      return;
    }
    throw new IllegalArgumentException("args = " + String.join(" ", args));
  }

  private static void printHelp() {
    System.out.print("""
        Usage: bach ...
        """);
  }
}
