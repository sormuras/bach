package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Main;
import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class BachToolProvider implements ToolProvider {

  public BachToolProvider() {}

  public String name() {
    return "bach";
  }

  public int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      return new Main().execute(out, err, args);
    } catch (RuntimeException exception) {
      return 1;
    }
  }

  @Override
  public String toString() {
    return "Build modular Java projects with JDK tools";
  }
}
