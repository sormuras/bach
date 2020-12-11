package com.github.sormuras.bach;

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Bach's main program. */
public class Main {
  /**
   * Main entry-point.
   *
   * @param args the command line arguments
   */
  public static void main(String... args) {
    var out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    var err = new PrintWriter(new OutputStreamWriter(System.err, StandardCharsets.UTF_8), true);
    int status = new Main().execute(out, err, args);
    System.exit(status);
  }

  /**
   * Print usage instructions to the given print stream.
   *
   * @param stream the stream to print to
   */
  public static void printUsageInstructions(PrintStream stream) {
    var out = new PrintWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8), true);
    new Main().help(out);
  }

  /** Default constructor. */
  public Main() {}

  /**
   * Main program.
   *
   * @param out the writer for normal messages, i.e. expected output
   * @param err the writer for error messages
   * @param args the command line arguments
   * @return a zero indicates normal execution, a non-zero value indicates abnormal termination
   */
  public int execute(PrintWriter out, PrintWriter err, String... args) {
    if (args.length == 0) {
      out.println("No argument, no action.");
      return 0;
    }
    var action = args[0];
    return switch (action) {
      case "build" -> build(out, err, Arrays.copyOfRange(args, 1, args.length));
      case "help", "usage" -> help(out);
      case "version" -> version(out);
      default -> {
        err.println("Unsupported action: " + action);
        yield 1;
      }
    };
  }

  int build(PrintWriter out, PrintWriter err, String... args) {
    try {
      var book = new Logbook(out::println, Logbook.defaultThresholdLevel());
      var bach = new Bach(book, Bach::newHttpClient);
      BuildProgram.execute(bach, args);
      return 0;
    } catch (RuntimeException exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }

  int help(PrintWriter out) {
    out.println("Usage: bach ACTION [ARGS...]");
    return 0;
  }

  int version(PrintWriter out) {
    out.println(Bach.version());
    return 0;
  }
}
