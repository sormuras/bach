package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Printer;
import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class BachToolProvider implements ToolProvider {
  @Override
  public String name() {
    return "bach";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var printer = Printer.of(out, err);
    var options = Options.ofCommandLineArguments(args);
    return Bach.run(printer, options);
  }
}
