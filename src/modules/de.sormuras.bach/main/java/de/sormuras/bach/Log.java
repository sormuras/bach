package de.sormuras.bach;

import java.io.PrintWriter;

/*BODY*/
/** Simplistic logging support. */
public /*STATIC*/ class Log {

  /** Create new Log instance using system default text output streams. */
  public static Log ofSystem() {
    var verbose = Boolean.getBoolean("verbose");
    var debug = Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug"));
    return ofSystem(verbose || debug);
  }

  /** Create new Log instance using system default text output streams. */
  public static Log ofSystem(boolean verbose) {
    return new Log(new PrintWriter(System.out, true), new PrintWriter(System.err, true), verbose);
  }

  /** Text-output writer. */
  /*PRIVATE*/ final PrintWriter out, err;
  /** Be verbose. */
  /*PRIVATE*/ final boolean verbose;

  public Log(PrintWriter out, PrintWriter err, boolean verbose) {
    this.out = out;
    this.err = err;
    this.verbose = verbose;
  }

  /** Print "debug" message to the standard output stream. */
  public void debug(String format, Object... args) {
    if (verbose) out.println(String.format(format, args));
  }

  /** Print "information" message to the standard output stream. */
  public void info(String format, Object... args) {
    out.println(String.format(format, args));
  }

  /** Print "warn" message to the error output stream. */
  public void warn(String format, Object... args) {
    err.println(String.format(format, args));
  }
}
