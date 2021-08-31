package com.github.sormuras.bach;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

/** Bach's main program. */
public record Main() implements ToolProvider {

  /** The main entry-point. */
  public static void main(String... args) {
    var options = args.length == 0 ? Options.parse("--help") : Options.parse(args);
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    new Program(out, err, options).run();
  }

  @Override
  public String name() {
    return "bach";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      new Program(out, err, Options.parse(args)).run();
      return 0;
    } catch (Exception exception) {
      err.println(exception);
      return 1;
    }
  }

  record Program(PrintWriter out, PrintWriter err, Options options) {

    public void run() {
      if (options.forMain().help().orElse(false)) {
        out.print("""
        Usage: bach ...
        """);
        return;
      }
      if (options.forMain().version().orElse(false)) {
        out.println(Bach.version());
        return;
      }
      if (options.forMain().generateBuildProgram().orElse(false)) {
        out.print(Bach.BuildProgramGenerators.generateConventionalBuildProgram());
        return;
      }
      throw new UnsupportedOperationException("options = %s".formatted(options));
    }
  }
}
