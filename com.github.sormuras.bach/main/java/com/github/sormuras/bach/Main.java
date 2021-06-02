package com.github.sormuras.bach;

/** Bach's main program. */
class Main {
  public static void main(String... args) {
    var printer = Printer.ofSystem();
    var options = Options.ofCommandLineArguments(args);
    var code = Bach.run(printer, options);
    if (code != 0) System.exit(code);
  }

  /** Hidden default constructor. */
  private Main() {}
}
