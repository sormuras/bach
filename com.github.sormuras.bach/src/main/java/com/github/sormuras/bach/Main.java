package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.BachLoader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.spi.ToolProvider;
import jdk.jfr.Recording;

/** Bach's main program. */
public record Main(String name) implements ToolProvider {

  /** Run an initial tool call and terminate the currently running VM on any error. */
  public static void main(String... args) {
    var code = provider().run(Printer.ofSystem(), args);
    if (code != 0) System.exit(code);
  }

  /** {@return an instance of this tool provider named {@code "bach"}} */
  public static Main provider() {
    return new Main("bach");
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    return run(new Printer(out, err), args);
  }

  /** {@return the result of running the initial tool call} */
  private int run(Printer printer, String... args) {
    var loader = new BachLoader(printer); // use internal loader to pre-process arguments
    var parser = loader.parser();
    var arguments = parser.parse(args);
    if (arguments.help().orElse(false)) {
      printer.out(parser.toHelp(name()));
      return 0;
    }
    if (arguments.command().isEmpty()) {
      printer.out(parser.toHelp(name()));
      printer.err("No initial command");
      return 1;
    }
    var bach = loader.load(arguments);
    return run(bach, ToolCall.of(arguments.command()));
  }

  /** {@return the result of running the initial tool call while recording events} */
  private int run(Bach bach, ToolCall call) {
    var printer = bach.configuration().printer();
    var verbose = bach.configuration().isVerbose();
    var output = bach.configuration().paths().out();
    try (var recording = new Recording()) {
      recording.start();
      try {
        if (verbose) printer.out("BEGIN");
        bach.run(call);
        if (verbose) printer.out("END.");
        return 0;
      } catch (RuntimeException exception) {
        printer.err(exception.getClass().getSimpleName() + ": " + exception.getMessage());
        exception.printStackTrace(printer.err());
        return 2;
      } finally {
        recording.stop();
        var jfr = Files.createDirectories(output).resolve("bach-logbook.jfr");
        recording.dump(jfr);
      }
    } catch (Exception exception) {
      exception.printStackTrace(printer.err());
      return -2;
    }
  }
}
