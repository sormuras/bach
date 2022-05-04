package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.StringPrintWriter;
import java.io.PrintWriter;

public record Printer(PrintWriter out, PrintWriter err) {

  public static Printer ofSilence() {
    var out = new StringPrintWriter();
    var err = new StringPrintWriter();
    return new Printer(out, err);
  }

  public static Printer ofSystem() {
    return ofSystem(true);
  }

  public static Printer ofSystem(boolean autoFlush) {
    var out = new PrintWriter(System.out, autoFlush);
    var err = new PrintWriter(System.err, autoFlush);
    return new Printer(out, err);
  }

  public void out(String string) {
    out.println(string);
  }

  public void err(String string) {
    err.println(string);
  }
}
