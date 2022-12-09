package run.bach;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.util.spi.ToolProvider;
import run.duke.ToolCall;
import run.duke.ToolNotFoundException;

public record Bach(Options options, Folders folders, Printer printer, Toolbox toolbox)
    implements ProjectToolRunner {
  public Bach {
    printer.log(Level.DEBUG, "Bach initialized");
    printer.log(Level.DEBUG, "  printer: " + printer.threshold());
    printer.log(Level.DEBUG, "  toolbox: " + toolbox.finders().list().size());
  }

  @Override
  public void run(ToolCall call) {
    printer().log(Level.INFO, "+ " + call.toCommandLine());
    var tool = call.name();
    var found = toolbox.finders().find(tool, this);
    if (found.isEmpty()) throw new ToolNotFoundException(tool);
    var args = call.arguments().toArray(String[]::new);
    var code = run(found.get().provider(), args);
    if (code != 0) throw new RuntimeException(tool + " failed with error " + code);
  }

  private int run(ToolProvider tool, String... args) {
    var silent = options().silent();
    try {
      var out = silent ? new PrintWriter(Writer.nullWriter()) : printer().out();
      var err = silent ? new PrintWriter(Writer.nullWriter()) : printer().err();
      return tool.run(out, err, args);
    } catch (Throwable throwable) {
      throwable.printStackTrace(System.err);
      return -1;
    }
  }
}
