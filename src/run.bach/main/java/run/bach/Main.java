package run.bach;

import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.spi.ToolProvider;
import run.bach.internal.CommandToolFinder;
import run.bach.internal.SourceModuleLayerBuilder;
import run.duke.ToolCall;
import run.duke.ToolCalls;
import run.duke.ToolFinder;
import run.duke.Toolbox;

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

    printer.log(Level.TRACE, "BEGIN");
    if (verbose) printer.out(options + " -> " + List.of(options.calls()));
    if (options.help()) {
      printer.out(
          """
          Usage: bach <options>... <command>
            <command>   -> <tool-call> [+ <tool-calls>...]
            <tool-call> -> <tool-name> <tool-args>...
            <options> are listed below""");
      printer.out(Options.toHelp().indent(4).stripTrailing());
      printer.out(
          """
          Examples
            bach --verbose jar --version + javac --version""");
      return 0;
    }

    printer.log(Level.DEBUG, "Building source module layer...");
    var layer = new SourceModuleLayerBuilder(Path.of(".bach")).build();
    printer.log(Level.DEBUG, "Source module layer contains: " + layer.modules());

    var commands = new TreeMap<String, Command>();
    for (var module : layer.modules()) {
      for (var command : module.getAnnotationsByType(Command.class)) {
        commands.put(module.getName() + '/' + command.name(), command);
      }
    }

    printer.log(Level.DEBUG, "Stuffing toolbox...");
    var toolbox =
        new Toolbox(layer)
            .with(new CommandToolFinder(commands)) // tool call shortcuts first
            .with("ToolProvider Finder", ServiceLoader.load(layer, ToolProvider.class))
            .with(ServiceLoader.load(layer, ToolFinder.class));

    printer.log(Level.DEBUG, "Creating sequence of initial tool calls...");
    var calls =
        options.calls().length == 0
            ? new ToolCalls(new ToolCall("list", List.of("tools")))
            : ToolCalls.of(options.calls());

    var folders = Folders.CURRENT_WORKING_DIRECTORY;
    var bach = new Bach(options, folders, printer, toolbox);

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
    printer.log(Level.TRACE, "END.");
    return 0;
  }
}
