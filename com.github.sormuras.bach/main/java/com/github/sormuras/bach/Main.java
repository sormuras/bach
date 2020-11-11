package com.github.sormuras.bach;

import static java.util.Arrays.copyOfRange;

import com.github.sormuras.bach.project.Base;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/** Bach's main program. */
public class Main {
  /**
   * Main entry-point.
   *
   * @param args the command line arguments
   */
  public static void main(String... args) {
    var out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
    var err = new PrintWriter(new OutputStreamWriter(System.err, StandardCharsets.UTF_8));
    int status = new Main().execute(out, err, args);
    System.exit(status);
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
      case "build" -> build(out, err, copyOfRange(args, 1, args.length));
      case "help", "usage" -> {
        out.println("Usage: bach ACTION [ARGS...]");
        yield 0;
      }
      case "version" -> {
        out.println(Bach.version());
        yield 0;
      }
      default -> {
        err.println("Unsupported action: " + action);
        yield 1;
      }
    };
  }

  int build(PrintWriter out, PrintWriter err, String... args) {
    try {
      var bach = new Bach(System.out, Bach::newHttpClient);
      var base = Base.ofCurrentDirectory();
      BuildProgram.build(bach, base, args);
      return 0;
    } catch (RuntimeException exception) {
      return 1;
    }
  }
}
