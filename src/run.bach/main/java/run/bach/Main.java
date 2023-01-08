package run.bach;

import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import run.bach.internal.SourceModuleLayerBuilder;
import run.duke.Duke;
import run.duke.ToolCalls;
import run.duke.Workbench;

public record Main() implements ToolProvider {
  public static void main(String... args) {
    var main = new Main();
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    var code = main.run(out, err, args);
    if (code != 0) System.exit(code);
  }

  @Override
  public String name() {
    return "bach";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var options = Options.of(args);
    var verbose = options.verbose();
    var silent = options.silent();
    var printer =
        new Printer(
            out,
            err,
            options.printerThreshold(verbose ? Level.ALL : silent ? Level.OFF : Level.INFO),
            options.printerMargin(1000));

    if (verbose) printer.out(options + " -> " + List.of(options.calls()));
    if (options.help()) {
      printer.out(
          """
          Usage: bach [<options>...] <tool-calls>
            <tool-calls> -> <tool-call> [+ <tool-calls>...]
            <tool-call>  -> <tool-name> [<tool-args>...]
            <options> are listed below""");
      printer.out(Options.toHelp().indent(4).stripTrailing());
      printer.out(
          """
          Examples
            bach --verbose jar --version + javac --version""");
      return 0;
    }
    var workbench = new Workbench(options, printer);

    var folders = Folders.of(options.rootDirectory(), options.outputDirectory(".bach/out"));
    workbench.put(folders);

    printer.log(Level.DEBUG, "Building source module layer...");
    var layer = SourceModuleLayerBuilder.of(folders.root(".bach")).build();
    var setting = new Setting(layer);
    workbench.put(setting);
    printer.log(Level.DEBUG, "Source module layer contains: " + layer.modules());

    printer.log(Level.DEBUG, "Loading composer...");
    var composer = ServiceLoader.load(layer, Composer.class).findFirst().orElseGet(Composer::new);
    var bach = composer.composeBach(workbench);

    printer.log(Level.DEBUG, "Creating sequence of initial tool calls...");
    var empty = options.calls().length == 0;
    var calls = empty ? ToolCalls.of(Duke.listTools()) : ToolCalls.of(options.calls());

    if (options.dryRun()) {
      if (verbose) printer.out("Dry-run mode exits here.");
      return 0;
    }
    if (verbose) {
      var size = calls.list().size();
      printer.log(Level.DEBUG, "Running %d tool call%s".formatted(size, size == 1 ? "" : "s"));
    }
    for (var call : calls) {
      bach.run(call);
    }
    return 0;
  }
}
