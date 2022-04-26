package com.github.sormuras.bach;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.spi.ToolProvider;

/** An extension of tool provider for running other tools in custom run implementations. */
@FunctionalInterface
public interface ToolOperator extends ToolProvider {
  @Override
  default String name() {
    return getClass().getSimpleName().toLowerCase(Locale.ROOT);
  }

  @Override
  default int run(PrintWriter out, PrintWriter err, String... args) {
    throw new UnsupportedOperationException();
  }

  @Override
  default int run(PrintStream out, PrintStream err, String... args) {
    throw new UnsupportedOperationException();
  }

  int run(Bach bach, PrintWriter out, PrintWriter err, String... args);
}
