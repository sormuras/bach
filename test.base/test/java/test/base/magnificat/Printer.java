package test.base.magnificat;

import java.io.PrintWriter;
import java.io.Writer;

public record Printer(PrintWriter out, PrintWriter err) {

  public static Printer of(PrintWriter out, PrintWriter err) {
    return new Printer(out, err);
  }

  public static Printer ofErrors() {
    return of(new PrintWriter(Writer.nullWriter()), new PrintWriter(System.err, true));
  }

  public static Printer ofSystem() {
    return ofSystem(true);
  }

  public static Printer ofSystem(boolean autoFlush) {
    return of(new PrintWriter(System.out, autoFlush), new PrintWriter(System.err, autoFlush));
  }
}
