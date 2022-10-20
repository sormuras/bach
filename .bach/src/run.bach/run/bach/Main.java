package run.bach;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import jdk.jfr.Recording;

/** Bach's main program. */
public record Main(String name) implements ToolProvider, ToolOperator {

  /** Run a sequence of tool calls and terminate the currently running VM on any error. */
  public static void main(String... args) {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    var code = provider().run(out, err, args);
    if (code != 0) System.exit(code);
  }

  /** {@return an instance of this tool provider/operator named {@code "bach"}} */
  public static Main provider() {
    return new Main("bach");
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    try (var recording = new Recording()) {
      var preliminaryCLI = new CLI().withParsingCommandLineArguments(List.of(args));
      var preliminaryPaths = Paths.ofRoot(preliminaryCLI.rootPath());
      var cli =
          new CLI()
              .withParsingCommandLineArguments(preliminaryPaths.root(".bach/bach.args"))
              .withParsingCommandLineArguments(List.of(args));
      var printer = new Printer(out, err, cli.printerThreshold(), cli.printerMargin());
      var verbose = cli.verbose();
      var version = cli.version();
      if (verbose || version) {
        out.println("Bach " + Bach.VERSION);
        if (version) return 0;
        out.println(cli.toString(0));
      }
      if (cli.help() || cli.calls().isEmpty()) {
        out.println(
            """
            Usage: bach [options] <tool> [args...] [+ <tool> [args...]]
            """);
        return 0;
      }
      try {
        recording.start();
        var bach = Bach.of(cli, printer);
        bach.runToolOperator(this, List.of(args));
        return 0;
      } finally {
        var dir = Files.createDirectories(preliminaryPaths.out());
        Files.writeString(dir.resolve("bach-printer.log"), printer.toHistoryString(0));
        recording.stop();
        recording.dump(dir.resolve("bach-events.jfr"));
      }
    } catch (Exception exception) {
      err.println(exception.getMessage());
      return 1;
    }
  }

  @Override
  public void operate(Bach bach, List<String> ignored) throws Exception {
    var calls = new ArrayList<>(bach.cli().calls());
    var first = calls.remove(0);
    bach.run(bach.tools(), ToolCall.of(first.command()), System.Logger.Level.DEBUG);
    for (var next : calls) bach.run(ToolCall.of(next.command()));
  }
}
