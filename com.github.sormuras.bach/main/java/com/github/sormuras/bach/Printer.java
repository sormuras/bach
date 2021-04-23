package com.github.sormuras.bach;

import java.io.PrintWriter;
import java.io.Writer;

/** A printer prints formatted representations of objects to text-output streams. */
public record Printer(PrintWriter out, PrintWriter err) {

  public static Printer of() {
    return Printer.of(new PrintWriter(Writer.nullWriter()));
  }

  public static Printer of(PrintWriter writer) {
    return Printer.of(writer, writer);
  }

  public static Printer of(PrintWriter out, PrintWriter err) {
    return new Printer(out, err);
  }

  public static Printer ofSystem() {
    return Printer.ofSystem(true);
  }

  public static Printer ofSystem(boolean autoFlush) {
    var out = new PrintWriter(System.out, autoFlush);
    var err = new PrintWriter(System.err, autoFlush);
    return Printer.of(out, err);
  }
}
