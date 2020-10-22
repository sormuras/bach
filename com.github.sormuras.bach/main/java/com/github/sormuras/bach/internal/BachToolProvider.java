package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Main;
import java.io.PrintWriter;
import java.util.spi.ToolProvider;

/**
 * An implementation of the {@link java.util.spi.ToolProvider ToolProvider} SPI, providing access to
 * the Java Shell Builder, {@code bach}.
 */
public class BachToolProvider implements ToolProvider {

  public BachToolProvider() {}

  @Override
  public String name() {
    return "bach";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      new Main(out::println, err::println).run(args);
      return 0;
    } catch (Throwable throwable) {
      throwable.printStackTrace(err);
      return -1;
    }
  }

  @Override
  public String toString() {
    return "Build modular Java projects";
  }
}
