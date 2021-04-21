package com.github.sormuras.bach;

import java.io.PrintWriter;

/** A printer prints formatted representations of objects to text-output streams. */
public record Printer(PrintWriter out, PrintWriter err) {

  public static Printer of(PrintWriter out, PrintWriter err) {
    return new Printer(out, err);
  }

  public static Printer ofSystem() {
    return ofSystem(true);
  }

  public static Printer ofSystem(boolean autoFlush) {
    return of(new PrintWriter(System.out, autoFlush), new PrintWriter(System.err, autoFlush));
  }
}
